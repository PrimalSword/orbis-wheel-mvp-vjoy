from __future__ import annotations

import csv
from datetime import datetime, timezone
from pathlib import Path

from ..config import FeedConfig
from ..models import Bar
from .base import MarketDataProvider


class CsvProvider(MarketDataProvider):
    name = "csv"

    def __init__(self, config: FeedConfig) -> None:
        self.config = config

    def connect(self) -> None:
        self.config.csv_directory.mkdir(parents=True, exist_ok=True)

    def close(self) -> None:
        return None

    def _path(self, symbol: str, timeframe: str) -> Path:
        filename = self.config.csv_pattern.format(symbol=symbol.upper(), timeframe=timeframe.upper())
        return self.config.csv_directory / filename

    @staticmethod
    def _timestamp(value: str) -> int:
        raw = value.strip()
        if raw.isdigit():
            number = int(raw)
            return number // 1000 if number > 10_000_000_000 else number
        parsed = datetime.fromisoformat(raw.replace("Z", "+00:00"))
        if parsed.tzinfo is None:
            parsed = parsed.replace(tzinfo=timezone.utc)
        return int(parsed.astimezone(timezone.utc).timestamp())

    def fetch_bars(
        self,
        symbol: str,
        timeframe: str,
        *,
        start: datetime | None,
        end: datetime,
        limit: int,
    ) -> list[Bar]:
        path = self._path(symbol, timeframe)
        if not path.exists():
            raise FileNotFoundError(
                f"CSV não encontrado: {path}. Use colunas time/open/high/low/close e, opcionalmente, volume/spread."
            )

        start_ts = int(start.timestamp()) if start else None
        end_ts = int(end.timestamp())
        bars: list[Bar] = []
        with path.open("r", encoding="utf-8-sig", newline="") as handle:
            reader = csv.DictReader(handle)
            normalized = {name.lower().strip(): name for name in (reader.fieldnames or [])}
            required = {"time", "open", "high", "low", "close"}
            missing = required - set(normalized)
            if missing:
                raise ValueError(f"CSV {path.name} sem colunas obrigatórias: {', '.join(sorted(missing))}")

            for row in reader:
                ts = self._timestamp(row[normalized["time"]])
                if start_ts is not None and ts < start_ts:
                    continue
                if ts > end_ts:
                    continue
                bars.append(
                    Bar(
                        provider=self.name,
                        symbol=symbol,
                        timeframe=timeframe,
                        open_time=ts,
                        open=float(row[normalized["open"]]),
                        high=float(row[normalized["high"]]),
                        low=float(row[normalized["low"]]),
                        close=float(row[normalized["close"]]),
                        tick_volume=int(float(row.get(normalized.get("volume", ""), 0) or 0)),
                        spread=int(float(row.get(normalized.get("spread", ""), 0) or 0)),
                    )
                )
        bars.sort(key=lambda item: item.open_time)
        return bars[-limit:]
