$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root
$env:ORBIS_FEED_PROVIDER = "synthetic"
& .\.venv\Scripts\orbis-feed.exe doctor
& .\.venv\Scripts\orbis-feed.exe once
& .\.venv\Scripts\orbis-feed.exe status
