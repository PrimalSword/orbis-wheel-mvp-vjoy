from __future__ import annotations

import tempfile
import unittest
from datetime import datetime, timezone
from pathlib import Path

from orbis_feed.config import FeedConfig
from orbis_feed.providers import BinanceProvider, CsvProvider, YahooProvider, build_provider


class ProviderTest(unittest.TestCase):
    def test_factory_builds_new_providers(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            for name, expected in (("csv", CsvProvider), ("binance", BinanceProvider), ("yahoo", YahooProvider)):
                config = FeedConfig.load(environ={
                    "ORBIS_FEED_PROVIDER": name,
                    "ORBIS_FEED_HOME": temp_dir,
                    "ORBIS_FEED_SYMBOLS": "TEST",
                    "ORBIS_FEED_TIMEFRAMES": "M1",
                })
                self.assertIsInstance(build_provider(config), expected)

    def test_csv_provider_reads_iso_and_unix_timestamps(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            config = FeedConfig.load(environ={
                "ORBIS_FEED_PROVIDER": "csv",
                "ORBIS_FEED_HOME": temp_dir,
                "ORBIS_FEED_SYMBOLS": "EURUSD",
                "ORBIS_FEED_TIMEFRAMES": "M1",
            })
            config.ensure_directories()
            path = Path(config.csv_directory) / "EURUSD_M1.csv"
            path.write_text(
                "time,open,high,low,close,volume\n"
                "2026-07-28T12:00:00Z,1.10,1.12,1.09,1.11,100\n"
                "1785240060,1.11,1.13,1.10,1.12,120\n",
                encoding="utf-8",
            )
            provider = CsvProvider(config)
            with provider:
                bars = provider.fetch_bars(
                    "EURUSD",
                    "M1",
                    start=None,
                    end=datetime(2030, 1, 1, tzinfo=timezone.utc),
                    limit=10,
                )
            self.assertEqual(len(bars), 2)
            self.assertEqual(bars[0].tick_volume, 100)
            self.assertLess(bars[0].open_time, bars[1].open_time)


if __name__ == "__main__":
    unittest.main()
