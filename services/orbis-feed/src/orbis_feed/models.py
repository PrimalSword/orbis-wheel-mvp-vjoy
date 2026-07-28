from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timezone


_TIMEFRAME_SECONDS = {
    "M1": 60,
    "M2": 120,
    "M3": 180,
    "M4": 240,
    "M5": 300,
    "M6": 360,
    "M10": 600,
    "M12": 720,
    "M15": 900,
    "M20": 1_200,
    "M30": 1_800,
    "H1": 3_600,
    "H2": 7_200,
    "H3": 10_800,
    "H4": 14_400,
    "H6": 21_600,
    "H8": 28_800,
    "H12": 43_200,
    "D1": 86_400,
    "W1": 604_800,
}


def normalize_timeframe(value: str) -> str:
    normalized = value.strip().upper()
    if normalized not in _TIMEFRAME_SECONDS:
        allowed = ", ".join(_TIMEFRAME_SECONDS)
        raise ValueError(f"Timeframe inválido: {value!r}. Permitidos: {allowed}")
    return normalized


def timeframe_seconds(value: str) -> int:
    return _TIMEFRAME_SECONDS[normalize_timeframe(value)]


@dataclass(frozen=True, slots=True)
class Bar:
    provider: str
    symbol: str
    timeframe: str
    open_time: int
    open: float
    high: float
    low: float
    close: float
    tick_volume: int = 0
    spread: int = 0
    real_volume: int = 0

    def __post_init__(self) -> None:
        object.__setattr__(self, "provider", self.provider.strip().lower())
        object.__setattr__(self, "symbol", self.symbol.strip().upper())
        object.__setattr__(self, "timeframe", normalize_timeframe(self.timeframe))
        if self.open_time <= 0:
            raise ValueError("open_time deve ser Unix timestamp positivo")
        if min(self.open, self.high, self.low, self.close) <= 0:
            raise ValueError("Preços devem ser positivos")
        if self.high < max(self.open, self.close, self.low):
            raise ValueError("high inconsistente")
        if self.low > min(self.open, self.close, self.high):
            raise ValueError("low inconsistente")

    @property
    def open_datetime(self) -> datetime:
        return datetime.fromtimestamp(self.open_time, tz=timezone.utc)

    def is_closed(self, now_ts: int, grace_seconds: int = 2) -> bool:
        return self.open_time + timeframe_seconds(self.timeframe) + grace_seconds <= now_ts
