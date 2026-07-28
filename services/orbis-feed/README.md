# Orbis Feed — Fase 1

Serviço independente que coleta candles fechados, normaliza qualquer fonte no mesmo modelo `Bar`, grava os dados em SQLite e mantém estado de saúde para Scanner, Backtest, Memory e Dashboard.

## Providers disponíveis

- `synthetic`: validação offline e determinística;
- `csv`: importação universal de arquivos locais;
- `yahoo`: ações, ETFs, índices, câmbio e cripto conforme os tickers do Yahoo Finance;
- `binance`: criptomoedas pelo endpoint público de klines, sem chave;
- `mt5`: integração opcional com o terminal MetaTrader 5.

O restante do Orbis não depende do provider escolhido. Todos entregam o mesmo objeto `Bar` e usam o mesmo banco.

## Núcleo implementado

- múltiplos símbolos e timeframes;
- coleta inicial e incremental;
- sobreposição, upsert e recuperação de lacunas;
- descarte do candle ainda aberto;
- SQLite em WAL com chave idempotente;
- isolamento de falhas por símbolo/timeframe;
- heartbeat, estado por fluxo e logs rotativos;
- comandos `doctor`, `once`, `run` e `status`;
- inicialização automática no Windows;
- testes do banco, do serviço, da fábrica e do CSV.

## Instalação

No PowerShell, dentro de `services/orbis-feed`:

```powershell
Set-ExecutionPolicy -Scope Process Bypass
.\scripts\install.ps1
```

## Teste inicial offline

```powershell
.\scripts\test-synthetic.ps1
```

O resultado esperado contém `provider_ok: true` e cria:

```text
data/orbis_feed.db
data/heartbeat.json
logs/orbis_feed.log
```

## Teste online sem MetaTrader

### Binance

No `.env`:

```env
ORBIS_FEED_PROVIDER=binance
ORBIS_FEED_SYMBOLS=BTCUSDT,ETHUSDT
ORBIS_FEED_TIMEFRAMES=M1,M5,M15,H1
```

Depois:

```powershell
.\.venv\Scripts\orbis-feed.exe doctor
.\.venv\Scripts\orbis-feed.exe once
.\.venv\Scripts\orbis-feed.exe status
```

### Yahoo Finance

No `.env`:

```env
ORBIS_FEED_PROVIDER=yahoo
ORBIS_FEED_SYMBOLS=PETR4.SA,^BVSP,EURUSD=X,BTC-USD
ORBIS_FEED_TIMEFRAMES=M5,M15,H1,D1
```

O Yahoo limita o alcance de dados intradiários; para histórico longo, prefira `D1` ou importe CSV.

### CSV

Coloque os arquivos em `data/imports` com o padrão:

```text
EURUSD_M5.csv
PETR4.SA_D1.csv
```

Colunas obrigatórias:

```csv
time,open,high,low,close,volume,spread
2026-07-28T12:00:00Z,1.10,1.12,1.09,1.11,100,0
```

O campo `time` aceita ISO 8601, Unix em segundos ou Unix em milissegundos.

## Execução contínua

```powershell
.\scripts\run.ps1
```

Para iniciar com o Windows:

```powershell
.\scripts\install-startup-task.ps1
```

## Banco

A chave única dos candles é:

```text
provider + symbol + timeframe + open_time
```

Todos os timestamps são armazenados em Unix UTC.

## Limites

- Yahoo Finance é uma fonte de pesquisa/pessoal e pode limitar períodos intradiários;
- Binance cobre criptomoedas negociadas na própria exchange;
- CSV depende da qualidade e do fuso dos dados importados;
- MT5 continua opcional;
- o Feed não interpreta mercado, não gera sinais e não envia ordens.
