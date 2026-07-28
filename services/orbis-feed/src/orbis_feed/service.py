from __future__ import annotations

import json
import logging
import signal
import time
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from pathlib import Path
from threading import Event
from typing import Callable

from .config import FeedConfig
from .database import FeedDatabase
from .models import timeframe_seconds
from .providers.base import MarketDataProvider


@dataclass(frozen=True, slots=True)
class CollectionResult:
    symbol: str
    timeframe: str
    received: int
    closed: int
    written: int
    latest_open_time: int | None


class FeedService:
    def __init__(
        self,
        config: FeedConfig,
        database: FeedDatabase,
        provider: MarketDataProvider,
        *,
        clock: Callable[[], datetime] | None = None,
    ) -> None:
        self.config = config
        self.database = database
        self.provider = provider
        self.clock = clock or (lambda: datetime.now(timezone.utc))
        self.stop_event = Event()
        self.log = logging.getLogger("orbis_feed")

    def request_stop(self, *_args: object) -> None:
        self.stop_event.set()

    def install_signal_handlers(self) -> None:
        signal.signal(signal.SIGINT, self.request_stop)
        if hasattr(signal, "SIGTERM"):
            signal.signal(signal.SIGTERM, self.request_stop)

    def _start_for_stream(self, symbol: str, timeframe: str) -> datetime | None:
        latest = self.database.latest_open_time(self.provider.name, symbol, timeframe)
        if latest is None:
            return None
        overlap = timeframe_seconds(timeframe) * self.config.overlap_bars
        return datetime.fromtimestamp(max(0, latest - overlap), tz=timezone.utc)

    def collect_stream(self, symbol: str, timeframe: str, now: datetime | None = None) -> CollectionResult:
        current = (now or self.clock()).astimezone(timezone.utc)
        start = self._start_for_stream(symbol, timeframe)
        interval = timeframe_seconds(timeframe)
        fetch_end = current
        limit = self.config.bootstrap_bars
        if start is not None:
            # Lacunas grandes avançam em lotes consecutivos, sem saltar candles antigos.
            fetch_end = min(current, start + timedelta(seconds=interval * self.config.max_batch_bars))
            limit = self.config.max_batch_bars

        bars = self.provider.fetch_bars(
            symbol,
            timeframe,
            start=start,
            end=fetch_end,
            limit=limit,
        )
        now_ts = int(current.timestamp())
        closed_bars = [
            bar for bar in bars
            if bar.is_closed(now_ts, self.config.close_grace_seconds)
        ]
        written = self.database.upsert_bars(closed_bars)
        previous_latest = self.database.latest_open_time(self.provider.name, symbol, timeframe)
        latest = max((bar.open_time for bar in closed_bars), default=previous_latest)
        self.database.mark_success(
            self.provider.name,
            symbol,
            timeframe,
            latest,
            written,
        )
        return CollectionResult(
            symbol=symbol,
            timeframe=timeframe,
            received=len(bars),
            closed=len(closed_bars),
            written=written,
            latest_open_time=latest,
        )

    def collect_once(self) -> list[CollectionResult]:
        results: list[CollectionResult] = []
        now = self.clock().astimezone(timezone.utc)
        for symbol in self.config.symbols:
            for timeframe in self.config.timeframes:
                try:
                    result = self.collect_stream(symbol, timeframe, now)
                    results.append(result)
                    self.log.info(
                        "%s %s: recebidos=%s fechados=%s gravados=%s",
                        symbol,
                        timeframe,
                        result.received,
                        result.closed,
                        result.written,
                    )
                except Exception as exc:
                    self.database.mark_error(self.provider.name, symbol, timeframe, str(exc))
                    self.log.exception("Falha em %s %s: %s", symbol, timeframe, exc)
        self._write_heartbeat(now, results)
        return results

    def _write_heartbeat(self, now: datetime, results: list[CollectionResult]) -> None:
        payload = {
            "service": "orbis-feed",
            "version": 1,
            "provider": self.provider.name,
            "updated_at": now.isoformat(),
            "streams_ok": len(results),
            "streams_expected": len(self.config.symbols) * len(self.config.timeframes),
            "database": str(self.config.database_path),
        }
        path = Path(self.config.heartbeat_path)
        path.parent.mkdir(parents=True, exist_ok=True)
        temporary = path.with_suffix(path.suffix + ".tmp")
        temporary.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
        temporary.replace(path)

    def run_forever(self) -> None:
        self.install_signal_handlers()
        self.log.info(
            "Orbis Feed iniciado: provider=%s símbolos=%s timeframes=%s",
            self.provider.name,
            ",".join(self.config.symbols),
            ",".join(self.config.timeframes),
        )
        while not self.stop_event.is_set():
            started = time.monotonic()
            self.collect_once()
            elapsed = time.monotonic() - started
            wait_seconds = max(0.25, self.config.poll_seconds - elapsed)
            self.stop_event.wait(wait_seconds)
        self.log.info("Orbis Feed finalizado")
