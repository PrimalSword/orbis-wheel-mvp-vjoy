from __future__ import annotations

import sqlite3
from contextlib import contextmanager
from pathlib import Path
from typing import Iterable, Iterator

from .models import Bar


_SCHEMA = """
CREATE TABLE IF NOT EXISTS candles (
    provider TEXT NOT NULL,
    symbol TEXT NOT NULL,
    timeframe TEXT NOT NULL,
    open_time INTEGER NOT NULL,
    open REAL NOT NULL,
    high REAL NOT NULL,
    low REAL NOT NULL,
    close REAL NOT NULL,
    tick_volume INTEGER NOT NULL DEFAULT 0,
    spread INTEGER NOT NULL DEFAULT 0,
    real_volume INTEGER NOT NULL DEFAULT 0,
    collected_at INTEGER NOT NULL DEFAULT (unixepoch()),
    PRIMARY KEY (provider, symbol, timeframe, open_time)
);

CREATE INDEX IF NOT EXISTS idx_candles_lookup
ON candles(symbol, timeframe, open_time DESC);

CREATE TABLE IF NOT EXISTS collector_state (
    provider TEXT NOT NULL,
    symbol TEXT NOT NULL,
    timeframe TEXT NOT NULL,
    last_open_time INTEGER,
    last_success_at INTEGER,
    last_error_at INTEGER,
    last_error TEXT,
    rows_written INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (provider, symbol, timeframe)
);

CREATE TABLE IF NOT EXISTS feed_events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at INTEGER NOT NULL DEFAULT (unixepoch()),
    level TEXT NOT NULL,
    event TEXT NOT NULL,
    details TEXT
);
"""


class FeedDatabase:
    def __init__(self, path: str | Path) -> None:
        self.path = Path(path)
        self.path.parent.mkdir(parents=True, exist_ok=True)

    @contextmanager
    def connect(self) -> Iterator[sqlite3.Connection]:
        connection = sqlite3.connect(self.path, timeout=30)
        connection.row_factory = sqlite3.Row
        connection.execute("PRAGMA journal_mode=WAL")
        connection.execute("PRAGMA synchronous=NORMAL")
        connection.execute("PRAGMA foreign_keys=ON")
        connection.execute("PRAGMA busy_timeout=30000")
        try:
            yield connection
            connection.commit()
        except Exception:
            connection.rollback()
            raise
        finally:
            connection.close()

    def initialize(self) -> None:
        with self.connect() as connection:
            connection.executescript(_SCHEMA)

    def upsert_bars(self, bars: Iterable[Bar]) -> int:
        rows = [
            (
                bar.provider,
                bar.symbol,
                bar.timeframe,
                bar.open_time,
                bar.open,
                bar.high,
                bar.low,
                bar.close,
                bar.tick_volume,
                bar.spread,
                bar.real_volume,
            )
            for bar in bars
        ]
        if not rows:
            return 0
        with self.connect() as connection:
            before = connection.total_changes
            connection.executemany(
                """
                INSERT INTO candles (
                    provider, symbol, timeframe, open_time,
                    open, high, low, close, tick_volume, spread, real_volume
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(provider, symbol, timeframe, open_time) DO UPDATE SET
                    open=excluded.open,
                    high=excluded.high,
                    low=excluded.low,
                    close=excluded.close,
                    tick_volume=excluded.tick_volume,
                    spread=excluded.spread,
                    real_volume=excluded.real_volume,
                    collected_at=unixepoch()
                """,
                rows,
            )
            return connection.total_changes - before

    def latest_open_time(self, provider: str, symbol: str, timeframe: str) -> int | None:
        with self.connect() as connection:
            row = connection.execute(
                """
                SELECT MAX(open_time) AS value FROM candles
                WHERE provider=? AND symbol=? AND timeframe=?
                """,
                (provider.lower(), symbol.upper(), timeframe.upper()),
            ).fetchone()
            return int(row["value"]) if row and row["value"] is not None else None

    def mark_success(
        self,
        provider: str,
        symbol: str,
        timeframe: str,
        last_open_time: int | None,
        rows_written: int,
    ) -> None:
        with self.connect() as connection:
            connection.execute(
                """
                INSERT INTO collector_state (
                    provider, symbol, timeframe, last_open_time,
                    last_success_at, last_error_at, last_error, rows_written
                ) VALUES (?, ?, ?, ?, unixepoch(), NULL, NULL, ?)
                ON CONFLICT(provider, symbol, timeframe) DO UPDATE SET
                    last_open_time=excluded.last_open_time,
                    last_success_at=unixepoch(),
                    last_error_at=NULL,
                    last_error=NULL,
                    rows_written=collector_state.rows_written + excluded.rows_written
                """,
                (provider.lower(), symbol.upper(), timeframe.upper(), last_open_time, rows_written),
            )

    def mark_error(self, provider: str, symbol: str, timeframe: str, error: str) -> None:
        with self.connect() as connection:
            connection.execute(
                """
                INSERT INTO collector_state (
                    provider, symbol, timeframe, last_error_at, last_error
                ) VALUES (?, ?, ?, unixepoch(), ?)
                ON CONFLICT(provider, symbol, timeframe) DO UPDATE SET
                    last_error_at=unixepoch(),
                    last_error=excluded.last_error
                """,
                (provider.lower(), symbol.upper(), timeframe.upper(), error[:2_000]),
            )

    def add_event(self, level: str, event: str, details: str | None = None) -> None:
        with self.connect() as connection:
            connection.execute(
                "INSERT INTO feed_events(level, event, details) VALUES (?, ?, ?)",
                (level.upper(), event, details),
            )

    def summary(self) -> dict[str, object]:
        with self.connect() as connection:
            total = connection.execute("SELECT COUNT(*) AS n FROM candles").fetchone()["n"]
            newest = connection.execute("SELECT MAX(open_time) AS ts FROM candles").fetchone()["ts"]
            states = [dict(row) for row in connection.execute(
                """
                SELECT provider, symbol, timeframe, last_open_time,
                       last_success_at, last_error_at, last_error, rows_written
                FROM collector_state
                ORDER BY symbol, timeframe
                """
            ).fetchall()]
            return {"total_candles": total, "newest_open_time": newest, "streams": states}
