# Cost Estimate (Owners Phase)

| Função        | Mem (MB) | Dur (ms) | Invoc/mês | GB-s Formula | GB-s (calc) | Custo Lambda (USD) | Custo Invoc (USD) | Custo API GW (USD) | Observações |
|---------------|----------|----------|-----------|-------------|------------|--------------------|-------------------|--------------------|-------------|
| owners-create | 512      | 200      | 100000    | (512/1024)*(0.2)*100000    | TBD        | TBD                | =100000*0.20/1_000_000 | =100000*1.00/1_000_000 | Exemplo base |
| owners-list   | 512      | 120      | 150000    | (512/1024)*(0.12)*150000   | TBD        | TBD                | =150000*0.20/1_000_000 | =150000*1.00/1_000_000 | --- |
| owners-get    | 512      | 90       | 120000    | (512/1024)*(0.09)*120000   | TBD        | TBD                | =120000*0.20/1_000_000 | =120000*1.00/1_000_000 | --- |
| owners-update | 512      | 180      | 40000     | (512/1024)*(0.18)*40000    | TBD        | TBD                | =40000*0.20/1_000_000  | =40000*1.00/1_000_000  | --- |
| owners-delete | 512      | 110      | 30000     | (512/1024)*(0.11)*30000    | TBD        | TBD                | =30000*0.20/1_000_000  | =30000*1.00/1_000_000  | --- |
| TOTAL         | -        | -        | 440000    | SUM                         | TBD SUM    | SUM                | SUM                 | SUM                | Ajustar c/ dados reais |

## Fórmulas

* GB-s = (MemoryMB / 1024) * (DurationMs / 1000) * Invoc
* Custo Lambda = GB-s * 0.00001667
* Custo Invoc = Invoc * 0.20 / 1_000_000
* Custo API Gateway HTTP = Invoc * 1.00 / 1_000_000

Substitua `<calc>` após extrair Dur real (p95 ou média) do CloudWatch / Artillery.

## Observações

* Free tier (1M invocações + 400k GB-s) pode reduzir custo efetivo se dentro dos limites.
* SnapStart não adiciona custo direto; pode reduzir duração média e custo indireto.
* RDS Proxy tem custo por ACU/hora (não estimado aqui — adicionar quando tiver parâmetros reais de conexões simultâneas).
* Requisições HTTP API cobradas separadamente (tabela acima).

## Como obter dados reais

1. Executar cenários Artillery (cold/warm/burst) e capturar latências médias/p95.
2. Obter métricas `Duration` (Lambda) e `InitDuration` (para avaliar cold start).
3. Escolher: usar p95 para capacidade (worst-case) ou média para base de custo.
4. Preencher planilha e recalcular GB-s.

## AWS Pricing Calculator

* Inserir cada função com memória configurada e duração média estimada.
* Adicionar API Gateway HTTP perfil de requests.
* Adicionar RDS Proxy + RDS Instance (separadamente) para visão ponta-a-ponta.
