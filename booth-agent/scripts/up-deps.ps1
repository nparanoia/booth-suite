$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
Write-Host "Starting booth-agent middleware only (postgres:5433, redis:6380)..."
docker compose up -d postgres redis
docker compose ps
Write-Host ""
Write-Host "Jar on host, e.g.:"
Write-Host '  $env:JAVA_HOME="D:\JAVA\jdk17"; java -jar target\booth-agent-1.0.0-SNAPSHOT.jar'
Write-Host "Full stack in Docker (default): mvn -DskipTests package; docker compose up -d --build"
