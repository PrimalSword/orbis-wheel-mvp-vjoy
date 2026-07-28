from __future__ import annotations

import hashlib
import math
from datetime import datetime

from ..models import Bar, timeframe_seconds
from .base import MarketDataProvider


class SyntheticProvider(MarketDataProvider):
    """Fonte determinística para validar o coletor sem corretora ou internet."""

    name = "synthetic"

    def connect(self) -> None:
        return None

    def close(self) -> None:
        return None

    @staticmethod
    def _base_price(symbol: str) -> float:
        if symbol.upper().endswith("JPY"):
            return 150.0
        if symbol.upper().startswith("XAU"):
            return 2_400.0
        digest = hashlib.sha256(symbol.upper().encode()).digest()
        return 1.0 + int.from_bytes(digest[:2], "big") / 100_000

    def fetch_bars(
        self,
        symbol: str,
        timeframe: str,
        *,
        start: datetime | None,
        end: datetime,
        limit: int,
    ) -> list[Bar]:
        interval = timeframe_seconds(timeframe)
        end_ts = int(end.timestamp())
        last_open = end_ts - (end_ts % interval)
        first_open = (
            int(start.timestamp()) - (int(start.timestamp()) % interval)
            if start is not None
            else last_open - interval * (limit - 1)
        )
        first_open = max(first_open, last_open - interval * (limit - 1))

        base = self._base_price(symbol)
        precision = 2 if base > 100 else 5
        bars: list[Bar] = []
        for index, ts in enumerate(range(first_open, last_open + 1, interval)):
            phase = ts / max(interval, 1)
            drift = math.sin(phase / 17.0) * base * 0.001
            open_price = base + drift
            close_price = open_price + math.sin(phase / 3.0) * base * 0.0002
            amplitude = base * (0.00015 + abs(math.cos(phase / 5.0)) * 0.0001)
            high = max(open_price, close_price) + amplitude
            low = min(open_price, close_price) - amplitude
            bars.append(
                Bar(
                    provider=self.name,
                    symbol=symbol,
                    timeframe=timeframe,
                    open_time=ts,
                    open=round(open_price, precision),
                    high=round(high, precision),
                    low=round(low, precision),
                    close=round(close_price, precision),
                    tick_volume=100 + index,
                )
            )
        return bars[-limit:]
