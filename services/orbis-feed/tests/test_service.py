from __future__ import annotations

import tempfile
import unittest
from datetime import datetime, timezone
from pathlib import Path

from orbis_feed.config import FeedConfig
from orbis_feed.database import FeedDatabase
from orbis_feed.providers.synthetic import SyntheticProvider
from orbis_feed.service import FeedService


class FeedServiceTest(unittest.TestCase):
    def _config(self, root: Path) -> FeedConfig:
        return FeedConfig.load(
            environ={
                "ORBIS_FEED_PROVIDER": "synthetic",
                "ORBIS_FEED_SYMBOLS": "EURUSD",
                "ORBIS_FEED_TIMEFRAMES": "M1,M5",
                "ORBIS_FEED_HOME": str(root),
                "ORBIS_FEED_BOOTSTRAP_BARS": "20",
                "ORBIS_FEED_OVERLAP_BARS": "3",
                "ORBIS_FEED_CLOSE_GRACE_SECONDS": "0",
            }
        )

    def test_collect_once_stores_only_closed_bars_and_is_idempotent(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            config = self._config(root)
            database = FeedDatabase(config.database_path)
            database.initialize()
            provider = SyntheticProvider()
            fixed = datetime(2026, 7, 28, 12, 0, 30, tzinfo=timezone.utc)
            service = FeedService(config, database, provider, clock=lambda: fixed)

            with provider:
                first = service.collect_once()
                count_after_first = database.summary()["total_candles"]
                second = service.collect_once()
                count_after_second = database.summary()["total_candles"]

            self.assertEqual(len(first), 2)
            self.assertEqual(len(second), 2)
            self.assertGreater(count_after_first, 0)
            self.assertEqual(count_after_first, count_after_second)
            self.assertTrue(config.heartbeat_path.exists())


if __name__ == "__main__":
    unittest.main()
