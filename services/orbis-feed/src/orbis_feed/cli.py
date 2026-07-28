from __future__ import annotations

import argparse
import json
import logging
from dataclasses import asdict
from logging.handlers import RotatingFileHandler

from . import __version__
from .config import FeedConfig
from .database import FeedDatabase
from .providers import build_provider
from .service import FeedService


def configure_logging(config: FeedConfig, verbose: bool = False) -> None:
    config.ensure_directories()
    level = logging.DEBUG if verbose else logging.INFO
    formatter = logging.Formatter("%(asctime)s %(levelname)s %(name)s: %(message)s")
    root = logging.getLogger()
    root.setLevel(level)
    root.handlers.clear()

    console = logging.StreamHandler()
    console.setFormatter(formatter)
    root.addHandler(console)

    file_handler = RotatingFileHandler(
        config.log_path,
        maxBytes=5_000_000,
        backupCount=5,
        encoding="utf-8",
    )
    file_handler.setFormatter(formatter)
    root.addHandler(file_handler)


def _load(args: argparse.Namespace) -> tuple[FeedConfig, FeedDatabase]:
    config = FeedConfig.load(args.env_file)
    config.ensure_directories()
    database = FeedDatabase(config.database_path)
    database.initialize()
    configure_logging(config, args.verbose)
    return config, database


def command_once(args: argparse.Namespace) -> int:
    config, database = _load(args)
    provider = build_provider(config)
    with provider:
        results = FeedService(config, database, provider).collect_once()
    print(json.dumps([asdict(result) for result in results], ensure_ascii=False, indent=2))
    return 0 if results else 2


def command_run(args: argparse.Namespace) -> int:
    config, database = _load(args)
    provider = build_provider(config)
    with provider:
        FeedService(config, database, provider).run_forever()
    return 0


def command_status(args: argparse.Namespace) -> int:
    config, database = _load(args)
    print(json.dumps(database.summary(), ensure_ascii=False, indent=2))
    return 0


def command_doctor(args: argparse.Namespace) -> int:
    config, database = _load(args)
    checks: dict[str, object] = {
        "version": __version__,
        "provider": config.provider,
        "database": str(config.database_path),
        "database_ok": config.database_path.exists(),
        "symbols": config.symbols,
        "timeframes": config.timeframes,
    }
    try:
        provider = build_provider(config)
        with provider:
            sample = provider.fetch_bars(
                config.symbols[0],
                config.timeframes[0],
                start=None,
                end=FeedService(config, database, provider).clock(),
                limit=10,
            )
        checks["provider_ok"] = True
        checks["sample_bars"] = len(sample)
    except Exception as exc:
        checks["provider_ok"] = False
        checks["provider_error"] = str(exc)
    print(json.dumps(checks, ensure_ascii=False, indent=2))
    return 0 if checks.get("provider_ok") else 2


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="orbis-feed",
        description="Coletor local de candles do Orbis Trade.",
    )
    parser.add_argument("--env-file", default=".env", help="Arquivo .env (padrão: .env)")
    parser.add_argument("--verbose", action="store_true", help="Ativa logs detalhados")
    parser.add_argument("--version", action="version", version=__version__)
    subparsers = parser.add_subparsers(dest="command", required=True)

    subparsers.add_parser("once", help="Executa um único ciclo").set_defaults(handler=command_once)
    subparsers.add_parser("run", help="Executa continuamente").set_defaults(handler=command_run)
    subparsers.add_parser("status", help="Mostra o estado do banco").set_defaults(handler=command_status)
    subparsers.add_parser("doctor", help="Valida banco e provider").set_defaults(handler=command_doctor)
    return parser


def main(argv: list[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    return int(args.handler(args))
