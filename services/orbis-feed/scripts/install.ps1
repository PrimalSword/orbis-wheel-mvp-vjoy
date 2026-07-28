$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

if (-not (Get-Command py -ErrorAction SilentlyContinue)) {
    throw "Python Launcher (py) não encontrado. Instale Python 3.11+ x64."
}

py -3 -m venv .venv
& .\.venv\Scripts\python.exe -m pip install --upgrade pip
& .\.venv\Scripts\python.exe -m pip install -e .
& .\.venv\Scripts\python.exe -m pip install -r requirements-mt5.txt

if (-not (Test-Path .env)) {
    Copy-Item .env.example .env
}

Write-Host "Instalação concluída. Primeiro teste: .\scripts\test-synthetic.ps1"
