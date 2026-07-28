from __future__ import annotations

from datetime import datetime, timedelta, timezone

from ..config import FeedConfig
from ..models import Bar, normalize_timeframe
from .base import MarketDataProvider


_YAHOO_INTERVALS = {
    "M1": "1m",
    "M2": "2m",
    "M5": "5m",
    "M15": "15m",
    "M30": "30m",
    "H1": "1h",
    "D1": "1d",
    "W1": "1wk",
}


class YahooProvider(MarketDataProvider):
    name = "yahoo"

    def __init__(self, config: FeedConfig) -> None:
        self.config = config
        self._yf = None

    def connect(self) -> None:
        try:
            import yfinance as yf
        except ImportError as exc:
            raise RuntimeError(
                "Provider Yahoo não instalado. Execute 'pip install -r requirements-providers.txt'."
            ) from exc
        self._yf = yf

    def close(self) -> None:
        self._yf = None

    def fetch_bars(
        self,
        symbol: str,
        timeframe: str,
        *,
        start: datetime | None,
        end: datetime,
        limit: int,
    ) -> list[Bar]:
        if self._yf is None:
            raise RuntimeError("Yahoo provider não conectado")
        normalized = normalize_timeframe(timeframe)
        interval = _YAHOO_INTERVALS.get(normalized)
        if interval is None:
            raise ValueError(f"Yahoo não suporta o timeframe {normalized} neste provider")

        end_utc = end.astimezone(timezone.utc)
        if start is None:
            seconds = max(60, int((end_utc.timestamp()) / max(limit, 1)))
            del seconds
            start_utc = end_utc - timedelta(days=59 if normalized.startswith(("M", "H")) else max(365, limit * 2))
        else:
            start_utc = start.astimezone(timezone.utc)

        frame = self._yf.Ticker(symbol).history(
            start=start_utc,
            end=end_utc + timedelta(seconds=1),
            interval=interval,
            auto_adjust=self.config.yahoo_auto_adjust,
            actions=False,
            prepost=False,
            raise_errors=True,
        )
        if frame is None or frame.empty:
            return []

        bars: list[Bar] = []
        for index, row in frame.iterrows():
            timestamp = index.to_pydatetime()
            if timestamp.tzinfo is None:
                timestamp = timestamp.replace(tzinfo=timezone.utc)
            timestamp = timestamp.astimezone(timezone.utc)
            bars.append(
                Bar(
                    provider=self.name,
                    symbol=symbol,
                    timeframe=normalized,
                    open_time=int(timestamp.timestamp()),
                    open=float(row["Open"]),
                    high=float(row["High"]),
                    low=float(row["Low"]),
                    close=float(row["Close"]),
                    real_volume=int(float(row.get("Volume", 0) or 0)),
                )
            )
        bars.sort(key=lambda item: item.open_time)
        return bars[-limit:]
