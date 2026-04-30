# fe-deploy.ps1

# Stop the script if any command fails
$ErrorActionPreference = "Stop"

Write-Host "--- Starting Deployment  ---" -ForegroundColor Cyan

# 1. Build the Docker Image
Write-Host "Step 1: Building image" -ForegroundColor Yellow
docker compose up -d --build

# 2. Push to Docker Hub
# Note: Ensure you have run 'docker login' once before running this
Write-Host "Step 2: Pushing image to Docker Hub..." -ForegroundColor Yellow
docker push pavesadmin/intranet-rms:latest

Write-Host "--- Deployment Successful! ---" -ForegroundColor Green