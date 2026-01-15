# Petclinic Serverless Migration (Strangler Pattern)

Arquitetura: Java 17 + Spring Boot 3 + Spring Cloud Function + AWS Lambda (função por endpoint) + API Gateway HTTP API + VPC Privada + RDS Proxy (MySQL) + HikariCP (maximumPoolSize=5, minimumIdle=0) + AWS Powertools v2 + SnapStart.

## Objetivo
Migrar domínios do Spring PetClinic (Owners, Visits) para funções independentes a fim de comparar desempenho, escalabilidade, resiliência e custo, mantendo convivência com o monólito (Strangler Pattern) e reutilizando o mesmo banco MySQL.

## Escopo e Estrutura

```text
petclinic-serverless/
  domain/        # Entidades, serviços, portas (DDD/Bounded Contexts)
  infra-rds/     # Repositórios JDBC + DataSource amigável a Lambda (RDS Proxy)
  api-common/    # DTOs / envelopes / JSON util / métricas
  functions/     # Uma pasta por endpoint (Owners, Visits, ...)
  iac/           # Serverless Framework + dashboards/alarms
```

Matriz de migração:

### Owners

| Endpoint | Função |
|----------|--------|
| POST /owners | owners-create |
| GET /owners | owners-list |
| GET /owners/{id} | owners-get |
| PUT /owners/{id} | owners-update |
| DELETE /owners/{id} | owners-delete |

### Visits

| Endpoint | Função |
|----------|--------|
| POST /owners/{ownerId}/pets/{petId}/visits | visits-create |
| GET /owners/{ownerId}/pets/{petId}/visits | visits-list |
| GET /owners/{ownerId}/pets/{petId}/visits/{visitId} | visits-get |
| PUT /owners/{ownerId}/pets/{petId}/visits/{visitId} | visits-update |
| DELETE /owners/{ownerId}/pets/{petId}/visits/{visitId} | visits-delete |

## Padrões Arquiteturais

- Strangler Pattern: convivência com monólito enquanto rotas de Owners migram.
- DDD + Bounded Contexts: domínio Owners isolado em `domain/owner`.
- Bulkhead: HikariCP pequeno por função (5) + RDS Proxy isolando picos.
- Sidecar (lógico): observabilidade com Powertools (logs, tracing, métricas).
- Não aplicados: Retry com backoff, SQS (load leveling), Step Functions (chaining).

## Ambiente Stateless: Mitigações-chave

- Conexões: RDS Proxy para estabilizar/reutilizar; Hikari com limites para evitar tempestade de conexões.
- Config/Segredos: SSM/Secrets Manager (sem valores inline), rotação transparente.
- Observabilidade: Powertools com `@Logging`, `@Tracing`, `@Metrics` (namespace "Petclinic").
- VPC: funções em sub-redes privadas; acesso a RDS via SG; VPC Endpoints para Secrets/SSM (sem NAT público).
- Cold start: contexto Spring enxuto + SnapStart.

## Build & Deploy

Pré-requisitos: Node.js, Serverless Framework (`npx serverless`), AWS CLI configurado, JDK 17.

1. Empacotar artefatos Maven

```powershell
mvn -q -DskipTests package
```

1. Deploy (ajuste stage/region)

```powershell
npx serverless deploy --stage dev --region us-east-1
```

### Principais parâmetros (SSM/Secrets)


- `/petclinic/<stage>/db/jdbcUrl` (ex.: `jdbc:mysql://<rds-proxy-endpoint>:3306/petclinic`)
- `/petclinic/<stage>/db/username`
- `/petclinic/<stage>/db/password` (SecureString)

## Observabilidade (Powertools)

Anotações: `@Logging`, `@Tracing`, `@Metrics(captureColdStart = true, namespace = "Petclinic")`.

Variáveis:

```bash
POWERTOOLS_SERVICE_NAME=petclinic
POWERTOOLS_LOG_LEVEL=INFO
POWERTOOLS_LOGGER_LOG_EVENT=true
```

Dimensões de métricas definidas em `api-common`: `Operation`, `Endpoint`, `Stage`.
Métricas típicas: `Owners<Create|List|Get|Update|Delete>LatencyMs`, `Owners<...>Count`, `OwnersUpdateConflictCount`.
Dashboards/alarms via IaC em `iac/` (CloudWatch Dashboard JSON e YAML).

## Testes de Carga (k6)

Este repositório inclui scripts k6 reutilizáveis para monólito e serverless (parametrização por `BASE_URL`).

### Arquivos


- `load_tests/owners_loadtest.js`

Executar (PowerShell):

```powershell
$env:BASE_URL = 'https://<sua-api-base>'
k6 run .\load_tests\owners_loadtest.js
```

Cenários cobertos: Spike, Rampa, Soak e Pico; thresholds básicos de p95 e taxa de erro (ajuste no script conforme necessidade).

## Resultados (Resumo do TCC)

- Cold start: SnapStart reduziu a primeira invocação (ex.: ~5.130 ms → ~2.360 ms).
- Steady state CRUD: latências típicas serverless 70–84 ms; `list` otimizada de 12,3 s (monólito) para 154 ms (serverless, JDBC).
- Carga: p95 ~61 ms em pico moderado; saturação do RDS Proxy a ~270 req/s (30% erros) confirmou o risco de impedância com banco relacional.
- Custos (Owners apenas): VPC Endpoints + RDS Proxy foram a maior parcela do custo mensal total (~USD 149), sugerindo alternativas como Aurora Data API ou DynamoDB para reduzir custos fixos e eliminar VPC.

## Integração com o Banco de Dados

- VPC privada: funções em sub-redes privadas; DB sem IP público; SG da Lambda → SG do Proxy/DB.
- RDS Proxy: reduz conexões diretas e estabiliza picos; combina com Hikari pequeno por função (5/0) para efeito bulkhead.
- Segredos e parâmetros: AWS Secrets Manager e SSM Parameter Store; rotação sem rebuild.
- Persistência: Spring JDBC Template (no lugar de JPA) para reduzir cold start e memória; elimina N+1 na `list`.

## Troubleshooting

| Sintoma | Possível causa | Ação |
|---------|---------------|------|
| Timeout ao conectar DB | SG/Subnet incorretos | Validar VPC IDs e regras de SG |
| SnapStart não ativo | Recurso sem `ApplyOn=PublishedVersions` | Conferir template gerado pelo Serverless |
| p95 alto em pico | Saturação de conexões (Proxy/DB) | Ajustar pool, escalar DB ou considerar NoSQL |

## Licença e Proveniência

Baseado no Spring PetClinic (Apache-2.0). Código de migração e IaC são adaptações para ambiente serverless, mantendo a mesma licença. O domínio Owners foi extraído e migrado segundo o Strangler Pattern.

---
Este README resume a migração e os resultados práticos usados no TCC. Para detalhes, consulte os documentos em `docs/` (capítulo de resultados, ATAM e mitigações stateless).
