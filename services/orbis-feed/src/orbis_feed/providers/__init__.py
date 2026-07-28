from __future__ import annotations

from ..config import FeedConfig
from .base import MarketDataProvider
from .mt5 import MetaTrader5Provider
from .synthetic import SyntheticProvider


def build_provider(config: FeedConfig) -> MarketDataProvider:
    if config.provider == "mt5":
        return MetaTrader5Provider(config)
    if config.provider == "synthetic":
        return SyntheticProvider()
    raise ValueError(f"Provider não suportado: {config.provider}")


__all__ = ["MarketDataProvider", "MetaTrader5Provider", "SyntheticProvider", "build_provider"]
