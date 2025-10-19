param(
  [string]$Profile = "petclinic",
  [string]$Region = "sa-east-1",
  [string]$Name = "Petclinic-Dev-Owners",
  [string]$File = "./iac/dashboard-dev.json"
)

if (-Not (Test-Path $File)) { Write-Error "Dashboard file not found: $File"; exit 1 }

$resolved = Resolve-Path $File
aws cloudwatch put-dashboard `
  --dashboard-name $Name `
  --dashboard-body file://$resolved `
  --profile $Profile `
  --region $Region
