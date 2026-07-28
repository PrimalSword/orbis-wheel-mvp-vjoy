from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from orbis_feed.database import FeedDatabase
from orbis_feed.models import Bar


class FeedDatabaseTest(unittest.TestCase):
    def test_upsert_is_idempotent_and_updates_candle(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            database = FeedDatabase(Path(temp_dir) / "feed.db")
            database.initialize()
            original = Bar("synthetic", "EURUSD", "M1", 1_700_000_000, 1.1, 1.2, 1.0, 1.15)
            updated = Bar("synthetic", "EURUSD", "M1", 1_700_000_000, 1.1, 1.3, 1.0, 1.2)

            self.assertEqual(database.upsert_bars([original]), 1)
            self.assertEqual(database.upsert_bars([updated]), 1)
            summary = database.summary()

            self.assertEqual(summary["total_candles"], 1)
            self.assertEqual(database.latest_open_time("synthetic", "EURUSD", "M1"), 1_700_000_000)


if __name__ == "__main__":
    unittest.main()
