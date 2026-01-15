#!/bin/bash
# Script de teste local para funções Visits
# Simula requisições HTTP para as funções Lambda

BASE_URL="${BASE_URL:-http://localhost:3001}"
OWNER_ID=1
PET_ID=1
VISIT_ID=1

# Cores para output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Petclinic Serverless - Visits API Test ===${NC}\n"

# 1. Create Visit
echo -e "${BLUE}1. POST /owners/{ownerId}/pets/{petId}/visits${NC}"
curl -X POST "$BASE_URL/owners/$OWNER_ID/pets/$PET_ID/visits" \
  -H "Content-Type: application/json" \
  -d '{
    "visitDate": "2025-01-10",
    "description": "Checkup and vaccination"
  }' \
  -w "\nStatus: %{http_code}\n\n"

# 2. List Visits for Pet
echo -e "${BLUE}2. GET /owners/{ownerId}/pets/{petId}/visits${NC}"
curl -X GET "$BASE_URL/owners/$OWNER_ID/pets/$PET_ID/visits" \
  -w "\nStatus: %{http_code}\n\n"

# 3. Get Visit
echo -e "${BLUE}3. GET /owners/{ownerId}/pets/{petId}/visits/{visitId}${NC}"
curl -X GET "$BASE_URL/owners/$OWNER_ID/pets/$PET_ID/visits/$VISIT_ID" \
  -w "\nStatus: %{http_code}\n\n"

# 4. Update Visit
echo -e "${BLUE}4. PUT /owners/{ownerId}/pets/{petId}/visits/{visitId}${NC}"
curl -X PUT "$BASE_URL/owners/$OWNER_ID/pets/$PET_ID/visits/$VISIT_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "visitDate": "2025-01-10",
    "description": "Checkup, vaccination and heartworm test"
  }' \
  -w "\nStatus: %{http_code}\n\n"

# 5. Delete Visit
echo -e "${BLUE}5. DELETE /owners/{ownerId}/pets/{petId}/visits/{visitId}${NC}"
curl -X DELETE "$BASE_URL/owners/$OWNER_ID/pets/$PET_ID/visits/$VISIT_ID" \
  -w "\nStatus: %{http_code}\n\n"

echo -e "${GREEN}Tests completed!${NC}"
