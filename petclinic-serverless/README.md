---
# Petclinic Serverless Migration (Strangler Pattern)

Arquitetura: Java 17 + Spring Boot 3 + Spring Cloud Function + AWS Lambda (função por endpoint) + API Gateway HTTP API + RDS Proxy (MySQL) + Powertools Java v2 + SnapStart.

## Objetivo
Migrar endpoints REST do Spring Petclinic para funções independentes, permitindo evolução incremental (Strangler Pattern) e comparação de performance (SnapStart vs inicialização padrão e futura variante GraalVM).

## Estrutura

```text
petclinic-serverless/
  domain/        # Entidades, serviços, portas
  infra-rds/     # Repositórios JDBC + DataSource Lambda-friendly
  api-common/    # DTOs / Envelope de resposta / util JSON
  functions/     # Uma pasta por endpoint (Owners inicial)
  iac/           # serverless.yml + docs infra
```

## Matriz de Migração
| Endpoint | Função | Status |
|----------|--------|--------|
| POST /owners | owners-create | Migrado |
| GET /owners | owners-list | Migrado |
| GET /owners/{id} | owners-get | Migrado |
| PUT /owners/{id} | owners-update | Migrado |
| DELETE /owners/{id} | owners-delete | Migrado |
| (Demais recursos) | (a definir) | Planejado |

## Build & Deploy

1. Empacotar JARs:

```bash
mvn -q -DskipTests package
```

2. Deploy (ajuste stage/region conforme necessidade):

```bash
npx serverless deploy --stage dev --region us-east-1
```

## Variáveis / Parâmetros (SSM ou Secrets Manager)

- /petclinic/<stage>/db/jdbcUrl  (jdbc:mysql://<proxy-endpoint>:3306/petclinic)
- /petclinic/<stage>/db/username
- /petclinic/<stage>/db/password (SecureString)

## SnapStart
- `versionFunctions: true` no provider.
- Overrides CloudFormation adicionam `SnapStart: ApplyOn=PublishedVersions`.
- Evite materializar conexões no static init; DataSource inicializa lazily.
 - Passos para validar Logical IDs:
  1. `npx serverless package --stage dev`
  2. Abrir `.serverless/cloudformation-template-update-stack.json`
  3. Localizar recursos `AWS::Lambda::Function` (Owners*) e confirmar que nomes batem com:
    - `OwnersCreateLambdaFunction`
    - `OwnersListLambdaFunction`
    - `OwnersGetLambdaFunction`
    - `OwnersUpdateLambdaFunction`
    - `OwnersDeleteLambdaFunction`
  4. Ajustar caso divergente.

## RDS Proxy
Placeholders em `serverless.yml`:
- `<DB_INSTANCE_ID>`
- `<SECRET_ARN>`
- `<PRIVATE_SUBNET_ID_1|2>`
- `<LAMBDA_SG_ID>` / `<DB_PROXY_SG_ID>`

## Observabilidade (Powertools)
Anotações usadas: `@Logging`, `@Tracing`, `@Metrics(captureColdStart = true, namespace = "Petclinic")`.
Variáveis recomendadas:
```
POWERTOOLS_SERVICE_NAME=petclinic
POWERTOOLS_LOG_LEVEL=INFO
POWERTOOLS_LOGGER_LOG_EVENT=true
```

## Métricas sugeridas
- OwnersCreatedCount (Counter)
- OwnerLatency (Timer p50/p95 em CloudWatch via EMF)
- ColdStart (automático do Powertools com captureColdStart)
 - OwnersListedCount / OwnersGetCount / OwnersUpdatedCount / OwnersDeletedCount

### Métricas e Latência (Detalhado)
`MetricsSupport` adiciona dimensões: `Operation`, `Endpoint`, `Stage`. Timers criados:
- OwnersCreateLatencyMs / OwnersServiceCreateLatencyMs
- OwnersListLatencyMs / OwnersServiceListLatencyMs
- OwnersGetLatencyMs / OwnersServiceGetLatencyMs
- OwnersUpdateLatencyMs / OwnersServiceUpdateLatencyMs
- OwnersDeleteLatencyMs / OwnersServiceDeleteLatencyMs

Counters:
- OwnersCreatedCount, OwnersListedCount, OwnersGetCount, OwnersUpdatedCount, OwnersDeletedCount

Visualização:
1. CloudWatch Metrics: filtrar namespace Petclinic.
2. Dashboard: provisionado em `iac/cloudwatch-dash.yml`.
3. Exportar CSV: usar console Metrics ou CLI.

### Como medir cold start
1. Rodar cenário `scripts/artillery-cold.yml`.
2. CloudWatch Logs Insights query (Init Duration) conforme `scripts/README-load.md`.
3. Comparar `InitDuration` com e sem SnapStart (desativar removendo overrides temporariamente).
4. Validar se latências p95 reduzem após warm.

### Cargas de teste
Ver `scripts/README-load.md` (cold, warm, burst).

### Custos
Ver `docs/cost-estimate.md` — inclui fórmulas e placeholders.

### Validação & Erros
`ValidationSupport` usa Bean Validation; erros mapeados por `ExceptionMapper` para payload consistente.

### IaC & Segurança
`serverless.yml` contém comentários para:
- Preencher Subnets / SG / Secret ARN.
- Verificar Logical IDs de SnapStart (`serverless package`).
- Restringir secretsmanager:GetSecretValue ao ARN específico.

### Próximos passos
1. Adicionar agregados: pets, visits, vets, specialties.
2. Criar módulo compartilhado para DTOs e padronização de erros.
3. Adicionar dashboards adicionais (erro vs sucesso, latency histogram).
4. Implementar warming strategy opcional (EventBridge Scheduler) se necessário.
5. Branch de comparação GraalVM native.

### Execução Artillery
Arquivo: `scripts/artillery-owners.yml`
Substituir `<API_GATEWAY_BASE>` pela URL base da HTTP API (ex: `https://abc123.execute-api.us-east-1.amazonaws.com`).

```bash
npx artillery run scripts/artillery-owners.yml -o owners-report.json
```

Gerar sumário:
```bash
npx artillery report owners-report.json
```

Coletar `initDuration`:
1. Ir em CloudWatch Logs do grupo de cada função.
2. Filtrar por `Init Duration` nas primeiras invocações após um `PublishVersion`.

Comparar SnapStart ativado vs desativado:
1. Desativar temporariamente removendo bloco SnapStart (ou definindo ApplyOn: None) e redeploy.
2. Rodar novamente o Artillery (primeira fase registra cold starts).

## Testes (planejado)
- Unit: OwnerService com repo fake
- Integração: OwnerJdbcRepository + Testcontainers (MySQL)
- Carga: scripts Artillery (frio / morno / burst)

## Próximos Passos
1. Implementar funções restantes de Owners.
2. Adicionar módulos para pets / visits / vets / specialties.
3. Criar `api-common` métricas custom (wrapper Powertools).
4. Dashboard CloudWatch (logs + X-Ray + métricas custom) via IaC.
5. Script comparação SnapStart vs sem SnapStart (initDuration, p50/p95, custo). 
6. Branch experimental GraalVM (não no escopo inicial).

## Rollback / Strangler
Para rotas não migradas manter monólito original atrás de um API Gateway separado ou ALB; pode-se usar rewrite/proxy posterior (comentários adicionais podem ser inseridos no serverless.yml se necessário).

## Segurança
- Princípio de menor privilégio em IAM roles por função (refinar depois).
- Segredos somente via SSM/Secrets; nunca valores inline.

## Troubleshooting
| Sintoma | Possível causa | Ação |
|---------|---------------|------|
| Timeout ao conectar DB | SG/Subnet incorretos | Validar VPC IDs e SG regras de saída/entrada |
| SnapStart não habilitado | Logical ID divergente | Verificar nome real do recurso Lambda no template gerado |
| Latência alta no primeiro call | SnapStart não aplicado ou warming insuficiente | Checar `Init Duration` em CloudWatch Logs |

## Licença
Baseado em Spring Petclinic (licença Apache 2.0); adaptações para serverless mantêm mesma licença.

---
_Este README evoluirá conforme mais endpoints forem migrados._

## Dashboard Owners (CloudWatch)

Arquivos:

- `iac/dashboard-dev.json` (dashboard específico stage dev Owners)
- `iac/publish-dashboard.ps1` script de publicação

Publicar dashboard:

```powershell
cd ./petclinic-serverless
powershell -ExecutionPolicy Bypass -File .\iac\publish-dashboard.ps1 -Profile petclinic -Region sa-east-1 -Name Petclinic-Dev-Owners
```

Validação:

```powershell
aws cloudwatch get-dashboard --dashboard-name Petclinic-Dev-Owners --profile petclinic --region sa-east-1
```

`DashboardValidationMessages` deve estar vazio.

### Métricas usadas

Namespace `Petclinic`, dimensões: `Service=petclinic`, `Operation=Owners`, `Endpoint=<ROTA>`, `Stage=dev`.

Principais métricas (handler-level):

- Create: `OwnersCreateLatencyMs`, `OwnersCreatedCount`, `OwnersCreateConflictCount`
- Update: `OwnersUpdateLatencyMs`, `OwnersUpdateSuccessCount`, `OwnersUpdateConflictCount`, `OwnersUpdateNotFoundCount`, `OwnersUpdateBadRequestCount`, `OwnersUpdateErrorCount`
- List/Get: `OwnersListLatencyMs`, `OwnersGetLatencyMs`
- Delete: `OwnersDeleteLatencyMs`, `OwnersDeleteSuccessCount`, `OwnersDeleteNotFoundCount` (mais legado `OwnersDeletedCount`)

Service-level (quando expostas): `OwnersService<Create|List|Get|Update|Delete>LatencyMs`.

### Logs Insights Queries

Create – erros e conflitos (60m):

```sql
fields @timestamp, @message
| filter @logGroup = "/aws/lambda/petclinic-serverless-dev-owners-create"
| filter @message like /OwnersCreate/ and (@message like /ERROR|WARN|Validation|Conflict/)
| sort @timestamp desc
| limit 100
```

Update – visão geral (60m):

```sql
fields @timestamp, @message
| filter @logGroup = "/aws/lambda/petclinic-serverless-dev-owners-update"
| filter @message like /OwnersUpdate/
| sort @timestamp desc
| limit 100
```

### Alarms (exemplos)

Criar p90 > 1s (Create) e conflitos Update >0:

```powershell
aws cloudwatch put-metric-alarm `
  --alarm-name "OwnersCreate-p90-High" `
  --namespace "Petclinic" `
  --metric-name "OwnersCreateLatencyMs" `
  --dimensions Name=Service,Value=petclinic Name=Operation,Value=Owners Name=Endpoint,Value=POST_/owners Name=Stage,Value=dev `
  --statistic p90 `
  --period 60 `
  --evaluation-periods 5 `
  --threshold 1000 `
  --comparison-operator GreaterThanThreshold `
  --treat-missing-data notBreaching `
  --region sa-east-1

aws cloudwatch put-metric-alarm `
  --alarm-name "OwnersUpdate-Conflict-Detected" `
  --namespace "Petclinic" `
  --metric-name "OwnersUpdateConflictCount" `
  --dimensions Name=Service,Value=petclinic Name=Operation,Value=Owners Name=Endpoint,Value=PUT_/owners/{id} Name=Stage,Value=dev `
  --statistic Sum `
  --period 60 `
  --evaluation-periods 5 `
  --threshold 0 `
  --comparison-operator GreaterThanThreshold `
  --treat-missing-data notBreaching `
  --region sa-east-1
```

### Checklist pós-publicação

1. Dashboard aparece em CloudWatch > Dashboards.
2. Gráficos começam a receber pontos após tráfego.
3. Logs widget exibe últimas linhas de create/update.
4. Gerar um conflito (telefone duplicado) e ver métrica `OwnersUpdateConflictCount` > 0 e alarm entrar em ALARM.
5. Confirmar SnapStart reduz Init Duration próximos de 0 ms nas novas invocações.
