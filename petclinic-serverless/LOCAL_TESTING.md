# Testando Serverless Localmente com serverless-offline

## **Pré-requisitos**
- Node.js 16+ (verificar: `node --version`)
- Java 17+ 
- Maven 3.8+
- JAR compilados (ver passo 2)

## **Passos para Testar Localmente**

### **1. Compilar os JARs**
```powershell
cd petclinic-serverless
mvn clean package -DskipTests
```

### **2. Iniciar serverless-offline**
```powershell
cd iac
npx serverless offline start
```

Você deve ver algo como:
```
Serverless: Registered 8 functions
Serverless: httpApi listening on http://localhost:3000
```

### **3. Testar Endpoints**

#### **Health Check (GET /)**
```powershell
curl http://localhost:3000/
```

#### **Criar Owner (POST /owners)**
```powershell
$body = @{
    firstName = "John"
    lastName = "Doe"
    address = "123 Main St"
    city = "Springfield"
    telephone = "1234567890"
} | ConvertTo-Json

curl -X POST http://localhost:3000/owners `
  -H "Content-Type: application/json" `
  -d $body
```

#### **Listar Owners (GET /owners)**
```powershell
curl http://localhost:3000/owners?page=1&size=10
```

#### **Criar Pet (POST /owners/1/pets)**
```powershell
$body = @{
    name = "Whiskers"
    birthDate = "2022-03-15"
    ownerId = 1
    typeId = 1  # 1=cat, 2=dog, 3=hamster, 4=bird
} | ConvertTo-Json

curl -X POST http://localhost:3000/owners/1/pets `
  -H "Content-Type: application/json" `
  -d $body
```

#### **Atualizar Pet (PUT /owners/1/pets/1)**
```powershell
$body = @{
    id = 1
    name = "Whiskers Updated"
    birthDate = "2022-03-15"
    ownerId = 1
    typeId = 1
} | ConvertTo-Json

curl -X PUT http://localhost:3000/owners/1/pets/1 `
  -H "Content-Type: application/json" `
  -d $body
```

#### **Listar Vets (GET /vets)**
```powershell
curl http://localhost:3000/vets
```

#### **Criar Visita (POST /owners/1/pets/1/visits)**
```powershell
$body = @{
    visitDate = "2024-01-22"
    description = "Regular checkup"
    ownerId = 1
    petId = 1
} | ConvertTo-Json

curl -X POST http://localhost:3000/owners/1/pets/1/visits `
  -H "Content-Type: application/json" `
  -d $body
```

## **Usando Postman/Insomnia**

1. Importe a collection:
```json
{
  "info": {
    "name": "Petclinic Serverless",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Create Owner",
      "request": {
        "method": "POST",
        "url": "http://localhost:3000/owners",
        "header": [{"key": "Content-Type", "value": "application/json"}],
        "body": {
          "mode": "raw",
          "raw": "{\"firstName\":\"John\",\"lastName\":\"Doe\",\"city\":\"Springfield\"}"
        }
      }
    }
  ]
}
```

## **Variáveis de Ambiente (para H2 em memória)**

Padrão: H2 em memória (sem persistência)

Para usar MySQL local:
```powershell
$env:DB_JDBC_URL="jdbc:mysql://localhost:3306/petclinic"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="password"
```

## **Troubleshooting**

### **Erro: "Cannot load function"**
- Verificar se os JARs foram compilados: `ls functions/*/target/*.jar`
- Resetar cache: `rm -r node_modules/.cache`

### **Erro: "Connection refused" (Banco de dados)**
- Padrão é H2 em memória (sem DB externo necessário)
- Se quiser MySQL: `docker run -d -p 3306:3306 -e MYSQL_ROOT_PASSWORD=password mysql:8.0`

### **Slow startup**
- Primeira execução é mais lenta (carrega Spring Boot 3x)
- Próximas execuções são mais rápidas

## **Parar o servidor**
```powershell
Ctrl+C
```

## **Próximos Passos**
- Testar com JMeter para carga
- Integrar com CI/CD
- Validar com dados reais de staging
