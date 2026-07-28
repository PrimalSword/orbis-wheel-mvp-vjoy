from __future__ import annotations

import json
from datetime import datetime
from urllib.parse import urlencode
from urllib.request import Request, urlopen

from ..config import FeedConfig
from ..models import Bar, normalize_timeframe
from .base import MarketDataProvider


_BINANCE_INTERVALS = {
    "M1": "1m",
    "M3": "3m",
    "M5": "5m",
    "M15": "15m",
    "M30": "30m",
    "H1": "1h",
    "H2": "2h",
    "H4": "4h",
    "H6": "6h",
    "H8": "8h",
    "H12": "12h",
    "D1": "1d",
    "W1": "1w",
}


class BinanceProvider(MarketDataProvider):
    name = "binance"

    def __init__(self, config: FeedConfig) -> None:
        self.config = config

    def connect(self) -> None:
        return None

    def close(self) -> None:
        return None

    def fetch_bars(
        self,
        symbol: str,
        timeframe: str,
        *,
        start: datetime | None,
        end: datetime,
        limit: int,
    ) -> list[Bar]:
        normalized = normalize_timeframe(timeframe)
        interval = _BINANCE_INTERVALS.get(normalized)
        if interval is None:
            raise ValueError(f"Binance não suporta o timeframe {normalized} neste provider")

        remaining = max(1, limit)
        cursor = int(start.timestamp() * 1000) if start else None
        end_ms = int(end.timestamp() * 1000)
        bars: list[Bar] = []

        while remaining > 0:
            batch = min(1_000, remaining)
            params: dict[str, object] = {
                "symbol": symbol.upper().replace("/", ""),
                "interval": interval,
                "limit": batch,
                "endTime": end_ms,
            }
            if cursor is not None:
                params["startTime"] = cursor
            url = f"{self.config.binance_base_url}/api/v3/klines?{urlencode(params)}"
            request = Request(url, headers={"User-Agent": "OrbisTrade/0.2"})
            with urlopen(request, timeout=self.config.request_timeout_seconds) as response:
                payload = json.loads(response.read().decode("utf-8"))
            if not isinstance(payload, list):
                raise RuntimeError(f"Resposta inesperada da Binance: {payload}")
            if not payload:
                break

            for row in payload:
                bars.append(
                    Bar(
                        provider=self.name,
                        symbol=symbol,
                        timeframe=normalized,
                        open_time=int(row[0]) // 1000,
                        open=float(row[1]),
                        high=float(row[2]),
                        low=float(row[3]),
                        close=float(row[4]),
                        tick_volume=int(float(row[8])),
                        real_volume=int(float(row[5])),
                    )
                )
            remaining -= len(payload)
            next_cursor = int(payload[-1][0]) + 1
            if cursor is None or next_cursor <= cursor or len(payload) < batch:
                break
            cursor = next_cursor

        bars.sort(key=lambda item: item.open_time)
        return bars[-limit:]
