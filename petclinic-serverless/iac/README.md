# Petclinic Serverless Migration (Strangler Pattern)

This IaC module contains the Serverless Framework configuration for migrating Spring Petclinic REST endpoints to AWS Lambda using Spring Cloud Function (function-per-endpoint) with SnapStart, RDS Proxy and AWS Powertools for Java v2.

## Status (initial)

| Endpoint | Function | Status |
|----------|----------|--------|
| POST /owners | owners-create | Implemented |
| GET /owners | owners-list | TODO |
| GET /owners/{id} | owners-get | TODO |
| PUT /owners/{id} | owners-update | TODO |
| DELETE /owners/{id} | owners-delete | TODO |
| (Pets, Visits, Vets, Specialties ...) | ... | Planned |

## Deploy

1. Build modules:
   mvn -q -DskipTests package
2. Deploy:
   npx serverless deploy --stage dev

Ensure SSM parameters exist:
- /petclinic/dev/db/jdbcUrl
- /petclinic/dev/db/username
- /petclinic/dev/db/password (SecureString)

## SnapStart
All functions publish versions (`versionFunctions: true`) and SnapStart is enabled via CloudFormation override per Lambda function.

## Next Steps
- Add remaining Owners functions and replicate SnapStart override blocks.
- Implement metrics (count, latency) using Powertools custom metrics.
- Provide Artillery/Gatling load scripts for cold/warm/burst scenarios.
