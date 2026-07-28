# Orbis Feed — Fase 1

Serviço independente do aplicativo Android que coleta **candles fechados**, grava-os localmente em SQLite e mantém um estado de saúde para os módulos seguintes do Orbis Trade.

## Decisão de arquitetura

O modo de produção usa o feed da corretora via **MetaTrader 5 + pacote Python oficial da MetaQuotes**. Não existe envio de ordens neste módulo. O coletor apenas chama funções de leitura de barras; não há `order_send` nem automação de entrada.

O terminal pode ser iniciado automaticamente pelo `initialize()` do pacote e permanecer minimizado. Depois da primeira autenticação, o processo não exige gráfico aberto nem interação manual. O modo `synthetic` permite validar banco, logs, recuperação e agendamento sem corretora e sem internet.

## O que já está implementado

- múltiplos símbolos e timeframes;
- coleta inicial e incremental com sobreposição e recuperação paginada de lacunas;
- descarte do candle ainda aberto;
- SQLite em modo WAL, chave idempotente e atualização segura;
- isolamento de falhas: um símbolo com erro não interrompe os demais;
- estado por fluxo, histórico de erros, heartbeat JSON e logs rotativos;
- comandos `doctor`, `once`, `run` e `status`;
- tarefa do Windows para iniciar junto com a sessão;
- provider sintético determinístico para o primeiro teste.

## Instalação no mini PC (Windows 10/11 x64)

Abra PowerShell na pasta `services/orbis-feed`:

```powershell
Set-ExecutionPolicy -Scope Process Bypass
.\scripts\install.ps1
```

### Teste 1 — sem MetaTrader

```powershell
.\scripts\test-synthetic.ps1
```

O resultado esperado é `provider_ok: true`, gravação de candles em `data/orbis_feed.db` e criação de `data/heartbeat.json`.

### Teste 2 — feed real do MetaTrader 5

1. Instale o MT5 da corretora e faça login em conta demo ou real.
2. Copie `.env.example` para `.env` se o instalador ainda não tiver criado.
3. Altere:

```env
ORBIS_FEED_PROVIDER=mt5
ORBIS_FEED_SYMBOLS=EURUSD,GBPUSD,USDJPY,XAUUSD
ORBIS_FEED_TIMEFRAMES=M1,M5,M15,H1
```

Os nomes precisam ser exatamente os exibidos pela corretora. Algumas usam sufixos, como `EURUSD.a` ou `XAUUSDm`.

Depois execute:

```powershell
.\.venv\Scripts\orbis-feed.exe doctor
.\.venv\Scripts\orbis-feed.exe once
.\.venv\Scripts\orbis-feed.exe status
```

Se houver mais de uma instalação do MT5, informe o caminho em `ORBIS_MT5_PATH`.

### Rodar continuamente

```powershell
.\scripts\run.ps1
```

Para iniciar automaticamente após o login:

```powershell
.\scripts\install-startup-task.ps1
```

## Banco

Tabela principal: `candles`.

Chave única:

```text
provider + symbol + timeframe + open_time
```

O timestamp é Unix UTC. Essa padronização evita duplicidades e prepara o banco para Scanner, Backtest e Memory.

## Comandos

```powershell
orbis-feed doctor
orbis-feed once
orbis-feed run
orbis-feed status
```

## Limites desta fase

- O provider oficial da MetaQuotes funciona em Windows x86-64 e precisa de uma instalação do terminal MT5 conectada a uma corretora.
- O histórico disponível depende do servidor da corretora e da configuração “Máx. de barras no gráfico” do terminal.
- O módulo não interpreta mercado, não gera sinal e não executa ordem. Essas funções pertencem às fases posteriores.
