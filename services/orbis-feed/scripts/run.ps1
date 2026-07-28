$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root
& .\.venv\Scripts\orbis-feed.exe run
