# Load & Performance Scenarios (Owners Phase)

## Install

```bash
npm i -g artillery
```

## Scenarios

| File | Goal | Notes |
|------|------|-------|
| artillery-cold.yml | Medir cold starts / SnapStart benefício | Taxa muito baixa (1 req/min) |
| artillery-warm.yml | Latência steady-state (p50/p95) | 10 rps constantes |
| artillery-burst.yml | Pico / escalabilidade / throttles | Rampa 10 -> 100 rps em 60s |

## Run

```bash
artillery run scripts/artillery-cold.yml -o cold.json
artillery run scripts/artillery-warm.yml -o warm.json
artillery run scripts/artillery-burst.yml -o burst.json
```

Gerar relatório HTML:

```bash
artillery report cold.json
```

## Interpretação

- p50: mediana (latência típica)
- p95/p99: cauda; observar impacto de picos e possíveis throttles
- Cold start: primeira(s) requisição(ões) após inatividade; comparar Duration vs InitDuration (CloudWatch)
- SnapStart: validar redução de InitDuration em logs e métrica `InitDuration`.

## CloudWatch Logs Insights (InitDuration)

Query exemplo (ajuste group name):

```sql
fields @timestamp, @message
| filter @message like /Init Duration/
| sort @timestamp asc
| limit 50
```

## Comparando SnapStart ON vs OFF

1. Desabilite SnapStart removendo override (ou definindo ApplyOn: None) para uma função e faça deploy.
2. Execute `artillery-cold.yml`.
3. Reabilite SnapStart, novo deploy (publica nova versão) e repita.
4. Registre média de InitDuration e p95 de latência em planilha.

## Exportar para CSV

Use `artillery run ... --output run.json` e converta JSON -> CSV (jq) ou gere relatório HTML e exporte manualmente.

## Próximos cenários futuros

- Cenário read-heavy (GET dominante)
- Cenário mixed owners + pets (quando outros agregados migrados)
