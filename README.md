# 🐾 Spring PetClinic Serverless
[![AWS Lambda](https://img.shields.io/badge/AWS-Lambda-orange.svg)](https://aws.amazon.com/lambda/)
[![Spring Cloud Function](https://img.shields.io/badge/Spring-Cloud%20Function-green.svg)](https://spring.io/projects/spring-cloud-function)
[![Java](https://img.shields.io/badge/Java-17-red.svg)](https://openjdk.java.net/)

> Uma reimplementação moderna do clássico **Spring PetClinic** utilizando arquitetura **Serverless** baseada em funções AWS Lambda, demonstrando estratégias práticas para decomposição de sistemas monolíticos.

Este projeto é resultado de uma pesquisa acadêmica de Trabalho de Conclusão de Curso (TCC) que investigou a viabilidade técnica, os padrões arquiteturais e os trade-offs envolvidos na migração de aplicações monolíticas tradicionais para o paradigma **Function-as-a-Service (FaaS)**.

---

## 📋 Sobre o Projeto

O **PetClinic Serverless** é uma refatoração progressiva e fundamentada do domínio **Owners** do sistema [Spring PetClinic](https://github.com/spring-projects/spring-petclinic), transformando operações CRUD monolíticas em **funções AWS Lambda independentes**, escaláveis e resilientes.

### 🎯 Objetivos

- Demonstrar a aplicação prática do **Strangler Pattern** para migração incremental
- Validar a decomposição orientada a domínio baseada em **Domain-Driven Design (DDD)**
- Avaliar desempenho, escalabilidade e resiliência comparando arquiteturas monolítica vs. serverless
- Documentar um guia técnico replicável para modernização de sistemas legados

### 🔬 Contexto Acadêmico

Este repositório implementa a metodologia proposta no TCC **"Estratégias para a Decomposição de Monólitos em Funções Serverless"**, desenvolvido no curso de Engenharia de Software da UDESC/CEAVI (2025).

---

## 🏗️ Arquitetura

### Visão Geral

A solução implementa uma **arquitetura híbrida de transição**, onde funções serverless coexistem temporariamente com o monólito original por meio de roteamento inteligente no API Gateway:

```
┌─────────────────┐
│   API Gateway   │ ← Ponto único de entrada (Strangler Façade)
└────────┬────────┘
         │
    ┌────┴────┐
    │         │
┌───▼──┐  ┌──▼────────────┐
│ Lambda│  │ Monólito      │
│ (NEW) │  │ (Legado)      │
└───┬───┘  └──┬────────────┘
    │         │
    └────┬────┘
         │
    ┌────▼────────┐
    │  RDS Proxy  │ ← Pool de conexões compartilhado (Bulkhead Pattern)
    └─────────────┘
         │
    ┌────▼────────┐
    │ MySQL (RDS) │
    └─────────────┘
```

### Padrões Arquiteturais Aplicados

| Padrão | Propósito | Implementação |
|--------|-----------|---------------|
| **Strangler Pattern** | Substituição gradual de funcionalidades | Roteamento seletivo no API Gateway |
| **Domain-Driven Design** | Decomposição por contextos delimitados | Extração do Bounded Context "Owners" |
| **Bulkhead Pattern** | Isolamento de recursos e contenção de falhas | RDS Proxy + pool Hikari limitado |
| **Sidecar Pattern** | Observabilidade como preocupação transversal | AWS Powertools for Java (logs, traces, métricas) |
| **Stateless Architecture** | Gestão de estado externalizada | AWS Secrets Manager + Parameter Store |

---

## ⚙️ Tecnologias Utilizadas

### 🖥️ Computação e Frameworks

- **[AWS Lambda](https://aws.amazon.com/lambda/)** - Plataforma FaaS para execução de funções sob demanda
- **[Spring Cloud Function](https://spring.io/projects/spring-cloud-function)** - Abstração para desenvolvimento de funções agnósticas de plataforma
- **[Spring Boot 3.2](https://spring.io/projects/spring-boot)** - Framework de aplicação com contexto minimizado
- **[AWS Lambda SnapStart](https://docs.aws.amazon.com/lambda/latest/dg/snapstart.html)** - Otimização de cold start para JVM (redução de ~51% no tempo de inicialização)

### 🌐 Interface, Roteamento e Persistência

- **[Amazon API Gateway](https://aws.amazon.com/api-gateway/)** - Gerenciamento de APIs REST com roteamento HTTP
- **[Amazon RDS (MySQL)](https://aws.amazon.com/rds/)** - Banco de dados relacional gerenciado
- **[Amazon RDS Proxy](https://aws.amazon.com/rds/proxy/)** - Pool de conexões gerenciado (mitigação de saturação)
- **[Spring JDBC Template](https://docs.spring.io/spring-framework/reference/data-access/jdbc.html)** - Acesso a dados leve (substituindo JPA para reduzir cold start)
- **[HikariCP](https://github.com/brettwooldridge/HikariCP)** - Pool de conexões JDBC de alta performance

### 🔐 Rede, Segurança e Gerenciamento de Acesso

- **[Amazon VPC](https://aws.amazon.com/vpc/)** - Isolamento de rede com sub-redes privadas
- **[VPC Interface Endpoints (PrivateLink)](https://docs.aws.amazon.com/vpc/latest/privatelink/)** - Comunicação privada com serviços AWS (Secrets Manager, SSM)
- **[AWS IAM](https://aws.amazon.com/iam/)** - Controle de acesso baseado em identidade (least privilege)
- **[AWS Secrets Manager](https://aws.amazon.com/secrets-manager/)** - Gerenciamento seguro de credenciais de banco de dados
- **[AWS KMS](https://aws.amazon.com/kms/)** - Criptografia de segredos e snapshots do SnapStart

### 📊 Observabilidade, Build e Testes

- **[Amazon CloudWatch](https://aws.amazon.com/cloudwatch/)** - Logs centralizados e métricas operacionais
- **[AWS X-Ray](https://aws.amazon.com/xray/)** - Rastreamento distribuído (tracing) de requisições
- **[AWS Powertools for Java](https://docs.powertools.aws.dev/lambda/java/)** - Instrumentação de logs estruturados, traces e métricas
- **[Serverless Framework](https://www.serverless.com/)** - Infraestrutura como Código (IaC) para deploy automatizado
- **[Apache Maven](https://maven.apache.org/)** - Gerenciamento de dependências e build (fat-JAR)
- **[k6](https://k6.io/)** - Testes de carga e performance (spike, soak, peak tests)
- **[Postman](https://www.postman.com/)** - Testes funcionais e validação de API

---

## 🚀 Funcionalidades Migradas

O domínio **Owners** foi decomposto nas seguintes funções independentes:

| Função | Endpoint | Método | Descrição |
|--------|----------|--------|-----------|
| `owners-list` | `/owners` | GET | Listagem paginada de proprietários |
| `owners-get` | `/owners/{id}` | GET | Detalhes de um proprietário específico |
| `owners-create` | `/owners` | POST | Cadastro de novo proprietário |
| `owners-update` | `/owners/{id}` | PUT | Atualização de dados cadastrais |
| `owners-delete` | `/owners/{id}` | DELETE | Remoção lógica de proprietário |

Cada função é:
- ✅ **Autônoma**: empacotada, implantada e escalada independentemente
- ✅ **Stateless**: estado externalizado (banco de dados + Secrets Manager)
- ✅ **Observável**: rastreamento distribuído com X-Ray e métricas no CloudWatch
- ✅ **Resiliente**: pool de conexões limitado e retry com backoff exponencial

---

## 📦 Estrutura do Projeto

```
petclinic-serverless/
├── src/main/java/org/springframework/samples/petclinic/
│   ├── owners/
│   │   ├── functions/          # Funções Lambda (Spring Cloud Function)
│   │   │   ├── OwnerCreateFunction.java
│   │   │   ├── OwnerGetFunction.java
│   │   │   ├── OwnerListFunction.java
│   │   │   ├── OwnerUpdateFunction.java
│   │   │   └── OwnerDeleteFunction.java
│   │   ├── domain/              # Entidades e objetos de domínio
│   │   ├── repository/          # Camada de acesso a dados (JDBC)
│   │   └── config/              # Configurações (DataSource, Observability)
│   └── ServerlessApplication.java
├── serverless.yml               # Configuração de infraestrutura (IaC)
├── pom.xml                      # Dependências Maven
└── docs/
    └── architecture/            # Diagramas e documentação técnica
```

---

## 🛠️ Como Executar

### Pré-requisitos

- **Java 17+** (OpenJDK ou Amazon Corretto)
- **Maven 3.8+**
- **AWS CLI** configurado com credenciais válidas
- **Serverless Framework** instalado globalmente:
  ```bash
  npm install -g serverless
  ```
- **Conta AWS** com permissões para Lambda, API Gateway, RDS, VPC, Secrets Manager

### 1️⃣ Configuração do Ambiente

Clone o repositório:
```bash
git clone https://github.com/nathalia-acordi/serverless-spring-petclinic.git
cd serverless-spring-petclinic/petclinic-serverless
```

Configure as variáveis de ambiente no arquivo `serverless.yml`:
```yaml
provider:
  environment:
    DB_SECRET_ARN: arn:aws:secretsmanager:us-east-1:123456789012:secret:petclinic-db-secret
    VPC_SUBNET_IDS: subnet-abc123,subnet-def456
    SECURITY_GROUP_ID: sg-0123456789abcdef
```

### 2️⃣ Build da Aplicação

Compile o projeto e gere o artefato JAR:
```bash
mvn clean package -DskipTests
```

### 3️⃣ Deploy na AWS

Implante a infraestrutura e as funções:
```bash
serverless deploy --stage dev --region us-east-1
```

Saída esperada:
```
✔ Service deployed to stack petclinic-serverless-dev (112s)

endpoints:
  GET    - https://abc123xyz.execute-api.us-east-1.amazonaws.com/dev/owners
  GET    - https://abc123xyz.execute-api.us-east-1.amazonaws.com/dev/owners/{id}
  POST   - https://abc123xyz.execute-api.us-east-1.amazonaws.com/dev/owners
  PUT    - https://abc123xyz.execute-api.us-east-1.amazonaws.com/dev/owners/{id}
  DELETE - https://abc123xyz.execute-api.us-east-1.amazonaws.com/dev/owners/{id}

functions:
  owners-list: petclinic-serverless-dev-owners-list (15 MB)
  owners-get: petclinic-serverless-dev-owners-get (15 MB)
  ...
```

### 4️⃣ Testes Funcionais

Teste a API utilizando curl ou Postman:
```bash
# Listar proprietários
curl https://abc123xyz.execute-api.us-east-1.amazonaws.com/dev/owners

# Buscar proprietário específico
curl https://abc123xyz.execute-api.us-east-1.amazonaws.com/dev/owners/1

# Criar novo proprietário
curl -X POST https://abc123xyz.execute-api.us-east-1.amazonaws.com/dev/owners \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Jane","lastName":"Doe","address":"123 Main St","city":"Springfield","telephone":"5551234567"}'
```

### 5️⃣ Testes de Carga (Opcional)

Execute os testes de performance com k6:
```bash
k6 run --vus 100 --duration 30s tests/load/spike-test.js
```

---

## 📊 Resultados Experimentais

### Desempenho (Cold Start)

| Métrica | Sem SnapStart | Com SnapStart | Melhoria |
|---------|---------------|---------------|----------|
| Latência média (1ª invocação) | 5.246 ms | 2.562 ms | **51,15%** |
| Tempo de restauração (RESTORE) | - | ~748 ms | - |

### Escalabilidade (Peak Load Test)

| Arquitetura | Vazão Máxima | Latência p95 | Taxa de Erros |
|-------------|--------------|--------------|---------------|
| **Monólito** | 3,13 req/s | 867 ms | 2,19% |
| **Serverless** | **270,5 req/s** | **61 ms** | 30,14%* |

*Os erros em alta concorrência ocorreram devido à saturação do banco de dados (gargalo conhecido), não das funções Lambda.

### Resiliência (Soak Test - 1 hora)

| Métrica | Monólito | Serverless |
|---------|----------|------------|
| Latência p95 | 31.918 ms (falha crítica) | **59 ms** (estável) |
| Vazão média | 2 req/s (degradação) | **120,5 req/s** |

**Conclusão**: A arquitetura serverless demonstrou elasticidade superior, mantendo latência estável mesmo sob carga prolongada, enquanto o monólito apresentou degradação severa.

---

## 🎓 Referências Acadêmicas

Este projeto implementa conceitos de:

- **FOWLER, M.** (2004). *Strangler Fig Application* - Padrão de migração incremental
- **EVANS, E.** (2003). *Domain-Driven Design* - Decomposição por contextos delimitados
- **NEWMAN, S.** (2015). *Building Microservices* - Estratégias de decomposição
- **BALDINI, I. et al.** (2017). *Serverless Computing: Current Trends and Open Problems*
- **BASS, L.; CLEMENTS, P.; KAZMAN, R.** (2013). *Software Architecture in Practice* - ATAM

---

## 🤝 Contribuindo

Contribuições são bem-vindas! Este projeto tem fins educacionais, mas melhorias na implementação, documentação ou testes são encorajadas.

1. Faça um fork do projeto
2. Crie uma branch para sua feature (`git checkout -b feature/nova-funcionalidade`)
3. Commit suas mudanças (`git commit -m 'Adiciona nova funcionalidade'`)
4. Push para a branch (`git push origin feature/nova-funcionalidade`)
5. Abra um Pull Request

---

## 👤 Autora

**Nathália Acordi da Silva**  
📧 Email: [nathalia.acordi@gmail.com](mailto:nathalia.acordi@gmail.com)  
🎓 Engenharia de Software - UDESC/CEAVI (2025)  
🔗 LinkedIn: [linkedin.com/in/nathalia-acordi](https://linkedin.com/in/nathalia-acordi)

---

## 🙏 Agradecimentos

- **Prof. Dr. Roberto Paulo Farah** - Orientador do TCC
- **UDESC/CEAVI** - Pelo suporte acadêmico

---

<div align="center">

**⭐ Se este projeto foi útil para sua pesquisa ou aprendizado, considere dar uma estrela no repositório!**


</div>
