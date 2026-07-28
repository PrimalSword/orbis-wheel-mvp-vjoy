# ORBISTRADE

Plataforma de análise técnica, backtest e apoio à decisão, sem execução automática de ordens.

## Objetivo

O Orbis Trade será construído em fases para:

1. coletar e armazenar dados reais de mercado;
2. escanear ativos e classificar oportunidades;
3. explicar tecnicamente cada cenário com assistência de IA;
4. comparar o mercado atual com ocorrências históricas;
5. integrar Backtest, Monte Carlo, Paper Trading e monitoramento em um painel único.

A decisão e a entrada continuam sendo do usuário. O projeto não executa operações automaticamente.

## Estrutura atual

### Aplicativo Android

Assistente Android desenvolvido em Java, com:

- telas e componentes visuais;
- modelos de candles e cotações;
- indicadores técnicos;
- estrutura e price action;
- regras, estratégias e score;
- gestão de risco;
- integração planejada com IA e scanner.

### Orbis Feed — Fase 1

Serviço para o mini PC responsável por coletar candles da corretora via MetaTrader 5 e armazená-los em SQLite, sem depender de chaves HTTP com limites baixos.

Documentação e comandos de teste:

- [`services/orbis-feed/README.md`](services/orbis-feed/README.md)
- [`ROADMAP.md`](ROADMAP.md)

## Arquitetura planejada

- `ui`: telas e componentes visuais;
- `data`: fontes locais e remotas;
- `network`: comunicação entre os módulos;
- `market`: modelos de candles e cotações;
- `indicators`: EMA, RSI, ATR e demais indicadores;
- `analysis`: regras, price action e score;
- `ai`: análise assistida e explicação;
- `risk`: cálculo de risco e tamanho de posição;
- `scanner`: varredura e ranking de ativos;
- `settings`: chaves e preferências seguras;
- `services/orbis-feed`: coleta permanente e banco local do mercado.

## Status

A Fase 1 está implementada no código e aguarda validação no mini PC e com o feed real da corretora. As fases seguintes permanecem bloqueadas até essa validação.

> Projeto educacional e de apoio à decisão. Não constitui recomendação financeira.
