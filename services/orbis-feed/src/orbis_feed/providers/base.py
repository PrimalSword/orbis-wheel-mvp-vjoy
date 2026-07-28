from __future__ import annotations

from abc import ABC, abstractmethod
from datetime import datetime

from ..models import Bar


class MarketDataProvider(ABC):
    name: str

    @abstractmethod
    def connect(self) -> None:
        pass

    @abstractmethod
    def close(self) -> None:
        pass

    @abstractmethod
    def fetch_bars(
        self,
        symbol: str,
        timeframe: str,
        *,
        start: datetime | None,
        end: datetime,
        limit: int,
    ) -> list[Bar]:
        pass

    def __enter__(self) -> "MarketDataProvider":
        self.connect()
        return self

    def __exit__(self, exc_type, exc, traceback) -> None:
        self.close()
