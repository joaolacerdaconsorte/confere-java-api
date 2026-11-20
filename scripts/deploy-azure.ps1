param(
    [string]$Location = "southafricanorth",
    [string]$ResourceGroup = "rg-confere-java",
    [string]$Plan = "plan-confere-java",
    [string]$WebApp = "confere-java-rm564969",
    [string]$Sku = "F1",
    [string]$LlmModel = "gemini-3.8-flash"
)

$ErrorActionPreference = "Stop"
$raiz = Split-Path -Parent $PSScriptRoot
Set-Location $raiz

function Passo {
    param([string]$Descricao, [scriptblock]$Comando)
    Write-Host ""
    Write-Host ">> $Descricao" -ForegroundColor Cyan
    & $Comando
    if ($LASTEXITCODE -ne 0) { throw "Falhou: $Descricao" }
}

function LerSegredo {
    param([string]$Pergunta)
    $seguro = Read-Host -Prompt $Pergunta -AsSecureString
    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($seguro)
    try { return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr) }
}

$conta = az account show --query "{assinatura:name, usuario:user.name}" -o tsv
Write-Host "Assinatura e usuario do az login: $conta" -ForegroundColor Yellow

$jwtSecret = LerSegredo "JWT_SECRET (a mesma chave da API .NET, minimo 32 caracteres)"
if ($jwtSecret.Length -lt 32) { throw "A JWT_SECRET precisa ter pelo menos 32 caracteres." }
$llmKey = LerSegredo "Chave do Gemini (LLM_API_KEY). Deixe vazio para usar a resposta padrao"
if ([string]::IsNullOrWhiteSpace($llmKey)) { $llmKey = "disabled" }

Passo "Gerando o .jar da API" { .\mvnw.cmd -q clean package -DskipTests }
$jar = Get-ChildItem target\*.jar | Where-Object { $_.Name -notlike "*plain*" } | Select-Object -First 1
if (-not $jar) { throw "Nao encontrei o .jar em target." }
Write-Host "Pacote: $($jar.Name)"

Passo "Registrando o provedor do App Service" { az provider register --namespace Microsoft.Web --wait }

Passo "Criando o Resource Group $ResourceGroup" {
    az group create --name $ResourceGroup --location $Location --tags projeto=confere api=java -o table
}

Passo "Criando o App Service Plan $Plan (Linux $Sku)" {
    az appservice plan create --name $Plan --resource-group $ResourceGroup --location $Location --is-linux --sku $Sku -o table
}

Passo "Criando o Web App $WebApp (Java 21)" {
    az webapp create --name $WebApp --resource-group $ResourceGroup --plan $Plan --runtime "JAVA:21-java21" -o table
}

Passo "Ligando HTTPS only, TLS 1.2 e desligando FTP" {
    az webapp update --name $WebApp --resource-group $ResourceGroup --https-only true -o none
    if ($LASTEXITCODE -ne 0) { return }
    az webapp config set --name $WebApp --resource-group $ResourceGroup --ftps-state Disabled --min-tls-version 1.2 -o none
}

Passo "Gravando as variaveis de ambiente" {
    az webapp config appsettings set --name $WebApp --resource-group $ResourceGroup -o none --settings `
        SERVER_PORT=80 `
        SPRING_PROFILES_ACTIVE=dev `
        H2_CONSOLE_ENABLED=false `
        RABBITMQ_AUTO_STARTUP=false `
        "JWT_SECRET=$jwtSecret" `
        "LLM_API_KEY=$llmKey" `
        LLM_BASE_URL=https://generativelanguage.googleapis.com/v1beta/openai `
        LLM_COMPLETIONS_PATH=/chat/completions `
        "LLM_MODEL=$LlmModel" `
        LLM_TIMEOUT=25s
}

Passo "Publicando o .jar no Web App" {
    az webapp deploy --resource-group $ResourceGroup --name $WebApp --src-path $jar.FullName --type jar
}

$url = "https://$WebApp.azurewebsites.net"
Write-Host ""
Write-Host ">> Esperando a API subir (pode levar ate 3 minutos)" -ForegroundColor Cyan
$ok = $false
for ($i = 1; $i -le 18; $i++) {
    try {
        $resp = Invoke-WebRequest -Uri "$url/actuator/health" -UseBasicParsing -TimeoutSec 30
        if ($resp.StatusCode -eq 200) { $ok = $true; break }
    } catch { }
    Write-Host "   tentativa ${i}: ainda subindo..."
    Start-Sleep -Seconds 10
}

Write-Host ""
if ($ok) {
    Write-Host "API no ar!" -ForegroundColor Green
} else {
    Write-Host "A API ainda nao respondeu. Veja o log com: az webapp log tail --name $WebApp --resource-group $ResourceGroup" -ForegroundColor Yellow
}
Write-Host "Swagger: $url/swagger-ui.html"
Write-Host "Health:  $url/actuator/health"
