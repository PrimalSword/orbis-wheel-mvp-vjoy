from __future__ import annotations

from datetime import datetime, timezone
from typing import Any

from ..config import FeedConfig
from ..models import Bar, normalize_timeframe
from .base import MarketDataProvider


class MetaTrader5Provider(MarketDataProvider):
    name = "mt5"

    def __init__(self, config: FeedConfig) -> None:
        self.config = config
        self._mt5: Any | None = None
        self._connected = False

    def _module(self) -> Any:
        if self._mt5 is not None:
            return self._mt5
        try:
            import MetaTrader5 as mt5
        except ImportError as exc:
            raise RuntimeError(
                "Pacote MetaTrader5 não instalado. No Windows, execute "
                "'pip install -r requirements-mt5.txt'."
            ) from exc
        self._mt5 = mt5
        return mt5

    def connect(self) -> None:
        mt5 = self._module()
        kwargs: dict[str, Any] = {
            "timeout": self.config.mt5_timeout_ms,
            "portable": self.config.mt5_portable,
        }
        if self.config.mt5_login is not None:
            kwargs["login"] = self.config.mt5_login
        if self.config.mt5_password:
            kwargs["password"] = self.config.mt5_password
        if self.config.mt5_server:
            kwargs["server"] = self.config.mt5_server

        if self.config.mt5_path:
            initialized = mt5.initialize(self.config.mt5_path, **kwargs)
        else:
            initialized = mt5.initialize(**kwargs)

        if not initialized:
            raise RuntimeError(f"Falha ao inicializar MetaTrader 5: {mt5.last_error()}")
        self._connected = True

    def close(self) -> None:
        if self._connected and self._mt5 is not None:
            self._mt5.shutdown()
        self._connected = False

    def _timeframe_constant(self, timeframe: str) -> Any:
        mt5 = self._module()
        normalized = normalize_timeframe(timeframe)
        name = f"TIMEFRAME_{normalized}"
        if not hasattr(mt5, name):
            raise ValueError(f"MetaTrader 5 não oferece o timeframe {normalized}")
        return getattr(mt5, name)

    def fetch_bars(
        self,
        symbol: str,
        timeframe: str,
        *,
        start: datetime | None,
        end: datetime,
        limit: int,
    ) -> list[Bar]:
        if not self._connected:
            raise RuntimeError("MetaTrader 5 não conectado")
        mt5 = self._module()
        symbol = symbol.upper()
        if not mt5.symbol_select(symbol, True):
            raise RuntimeError(
                f"Símbolo {symbol} indisponível no servidor da corretora: {mt5.last_error()}"
            )

        tf = self._timeframe_constant(timeframe)
        end_utc = end.astimezone(timezone.utc)
        if start is None:
            rates = mt5.copy_rates_from_pos(symbol, tf, 0, limit)
        else:
            rates = mt5.copy_rates_range(symbol, tf, start.astimezone(timezone.utc), end_utc)

        if rates is None:
            raise RuntimeError(
                f"MT5 não retornou candles para {symbol} {timeframe}: {mt5.last_error()}"
            )

        bars = [
            Bar(
                provider=self.name,
                symbol=symbol,
                timeframe=timeframe,
                open_time=int(row["time"]),
                open=float(row["open"]),
                high=float(row["high"]),
                low=float(row["low"]),
                close=float(row["close"]),
                tick_volume=int(row["tick_volume"]),
                spread=int(row["spread"]),
                real_volume=int(row["real_volume"]),
            )
            for row in rates
        ]
        bars.sort(key=lambda bar: bar.open_time)
        return bars[-limit:]
