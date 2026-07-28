from __future__ import annotations

from ..config import FeedConfig
from .base import MarketDataProvider
from .binance import BinanceProvider
from .csv import CsvProvider
from .mt5 import MetaTrader5Provider
from .synthetic import SyntheticProvider
from .yahoo import YahooProvider


def build_provider(config: FeedConfig) -> MarketDataProvider:
    providers = {
        "synthetic": lambda: SyntheticProvider(),
        "csv": lambda: CsvProvider(config),
        "yahoo": lambda: YahooProvider(config),
        "binance": lambda: BinanceProvider(config),
        "mt5": lambda: MetaTrader5Provider(config),
    }
    try:
        return providers[config.provider]()
    except KeyError as exc:
        raise ValueError(f"Provider não suportado: {config.provider}") from exc


__all__ = [
    "MarketDataProvider",
    "SyntheticProvider",
    "CsvProvider",
    "YahooProvider",
    "BinanceProvider",
    "MetaTrader5Provider",
    "build_provider",
]
