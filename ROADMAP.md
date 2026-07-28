# ORBISTRADE — ROADMAP CONGELADO

Regra: nenhuma fase seguinte começa antes da validação prática da fase atual.

## Fase 1 — Orbis Feed

- [x] Serviço independente para o mini PC
- [x] Núcleo desacoplado da fonte de dados
- [x] Provider sintético para validação offline
- [x] Provider CSV para importação universal e offline
- [x] Provider Yahoo Finance para ações, ETFs, índices, câmbio e cripto
- [x] Provider Binance para cripto via endpoint público, sem chave
- [x] Provider MetaTrader 5 como opção adicional
- [x] SQLite local em WAL
- [x] Múltiplos símbolos e timeframes
- [x] Coleta inicial e incremental
- [x] Sobreposição e upsert para recuperação de lacunas
- [x] Exclusão de candles ainda abertos
- [x] Logs rotativos, heartbeat e estado por fluxo
- [x] Instalação e inicialização automática no Windows
- [x] Testes automatizados do núcleo e dos providers
- [ ] **VALIDAÇÃO NO MINI PC**
- [ ] **VALIDAÇÃO COM AO MENOS UM FEED ONLINE SEM METATRADER**

## Fase 2 — Orbis Scanner

- [ ] Bloqueada até validação integral da Fase 1
- [ ] Indicadores e estrutura de mercado
- [ ] Estratégias e ranking de oportunidades
- [ ] Score e filtros de qualidade

## Fase 3 — Orbis AI

- [ ] Bloqueada
- [ ] Explicação dos sinais
- [ ] Justificativa técnica e avaliação de risco
- [ ] Assistente de decisão sem execução automática

## Fase 4 — Orbis Memory

- [ ] Bloqueada
- [ ] Busca de padrões historicamente semelhantes
- [ ] Probabilidade, retorno e drawdown esperados

## Fase 5 — Dashboard 3.0

- [ ] Bloqueada
- [ ] Home, Feed, Scanner, IA, Backtest e Paper Trading integrados
