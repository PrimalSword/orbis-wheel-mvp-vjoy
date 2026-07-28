from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path
from typing import Mapping

from .models import normalize_timeframe


def _parse_env_file(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    if not path.exists():
        return values
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        key = key.strip()
        value = value.strip().strip('"').strip("'")
        if key:
            values[key] = value
    return values


def _as_bool(value: str | None, default: bool = False) -> bool:
    if value is None:
        return default
    return value.strip().lower() in {"1", "true", "yes", "sim", "on"}


def _as_int(value: str | None, default: int, minimum: int = 0) -> int:
    if value is None or not value.strip():
        return default
    parsed = int(value)
    if parsed < minimum:
        raise ValueError(f"Valor {parsed} deve ser >= {minimum}")
    return parsed


def _csv(value: str, *, upper: bool = True) -> tuple[str, ...]:
    items = [item.strip() for item in value.split(",") if item.strip()]
    if upper:
        items = [item.upper() for item in items]
    return tuple(dict.fromkeys(items))


@dataclass(frozen=True, slots=True)
class FeedConfig:
    provider: str
    symbols: tuple[str, ...]
    timeframes: tuple[str, ...]
    database_path: Path
    log_path: Path
    heartbeat_path: Path
    poll_seconds: int
    bootstrap_bars: int
    overlap_bars: int
    max_batch_bars: int
    close_grace_seconds: int
    request_timeout_seconds: int
    csv_directory: Path
    csv_pattern: str
    binance_base_url: str
    yahoo_auto_adjust: bool
    mt5_path: str | None
    mt5_login: int | None
    mt5_password: str | None
    mt5_server: str | None
    mt5_timeout_ms: int
    mt5_portable: bool

    @classmethod
    def load(
        cls,
        env_file: str | Path | None = None,
        environ: Mapping[str, str] | None = None,
    ) -> "FeedConfig":
        source = dict(_parse_env_file(Path(env_file))) if env_file else {}
        source.update(dict(os.environ if environ is None else environ))

        provider = source.get("ORBIS_FEED_PROVIDER", "synthetic").strip().lower()
        allowed = {"synthetic", "csv", "yahoo", "binance", "mt5"}
        if provider not in allowed:
            raise ValueError(f"ORBIS_FEED_PROVIDER deve ser um de: {', '.join(sorted(allowed))}")

        symbols = _csv(source.get("ORBIS_FEED_SYMBOLS", "EURUSD,GBPUSD,USDJPY,XAUUSD"))
        if not symbols:
            raise ValueError("Configure ao menos um símbolo")

        timeframes = tuple(
            normalize_timeframe(item)
            for item in _csv(source.get("ORBIS_FEED_TIMEFRAMES", "M1,M5,M15,H1"))
        )
        if not timeframes:
            raise ValueError("Configure ao menos um timeframe")

        root = Path(source.get("ORBIS_FEED_HOME", ".")).expanduser().resolve()
        data_dir = root / "data"
        log_dir = root / "logs"
        login_raw = source.get("ORBIS_MT5_LOGIN", "").strip()

        return cls(
            provider=provider,
            symbols=symbols,
            timeframes=timeframes,
            database_path=Path(source.get("ORBIS_FEED_DB", data_dir / "orbis_feed.db")).expanduser().resolve(),
            log_path=Path(source.get("ORBIS_FEED_LOG", log_dir / "orbis_feed.log")).expanduser().resolve(),
            heartbeat_path=Path(source.get("ORBIS_FEED_HEARTBEAT", data_dir / "heartbeat.json")).expanduser().resolve(),
            poll_seconds=_as_int(source.get("ORBIS_FEED_POLL_SECONDS"), 5, 1),
            bootstrap_bars=_as_int(source.get("ORBIS_FEED_BOOTSTRAP_BARS"), 2_000, 10),
            overlap_bars=_as_int(source.get("ORBIS_FEED_OVERLAP_BARS"), 5, 1),
            max_batch_bars=_as_int(source.get("ORBIS_FEED_MAX_BATCH_BARS"), 100_000, 100),
            close_grace_seconds=_as_int(source.get("ORBIS_FEED_CLOSE_GRACE_SECONDS"), 2, 0),
            request_timeout_seconds=_as_int(source.get("ORBIS_FEED_REQUEST_TIMEOUT_SECONDS"), 20, 1),
            csv_directory=Path(source.get("ORBIS_CSV_DIRECTORY", data_dir / "imports")).expanduser().resolve(),
            csv_pattern=source.get("ORBIS_CSV_PATTERN", "{symbol}_{timeframe}.csv"),
            binance_base_url=source.get("ORBIS_BINANCE_BASE_URL", "https://api.binance.com").rstrip("/"),
            yahoo_auto_adjust=_as_bool(source.get("ORBIS_YAHOO_AUTO_ADJUST"), False),
            mt5_path=source.get("ORBIS_MT5_PATH") or None,
            mt5_login=int(login_raw) if login_raw else None,
            mt5_password=source.get("ORBIS_MT5_PASSWORD") or None,
            mt5_server=source.get("ORBIS_MT5_SERVER") or None,
            mt5_timeout_ms=_as_int(source.get("ORBIS_MT5_TIMEOUT_MS"), 60_000, 1_000),
            mt5_portable=_as_bool(source.get("ORBIS_MT5_PORTABLE"), False),
        )

    def ensure_directories(self) -> None:
        self.database_path.parent.mkdir(parents=True, exist_ok=True)
        self.log_path.parent.mkdir(parents=True, exist_ok=True)
        self.heartbeat_path.parent.mkdir(parents=True, exist_ok=True)
        self.csv_directory.mkdir(parents=True, exist_ok=True)
