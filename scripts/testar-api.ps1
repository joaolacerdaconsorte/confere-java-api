param(
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"
$BaseUrl = $BaseUrl.TrimEnd("/")
$script:ok = 0
$script:falhas = 0
$script:linhas = @()

function Chamar {
    param(
        [string]$Metodo,
        [string]$Rota,
        [string]$Token = $null,
        $Corpo = $null
    )
    $headers = @{}
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }
    $params = @{
        Uri             = "$BaseUrl$Rota"
        Method          = $Metodo
        Headers         = $headers
        UseBasicParsing = $true
        TimeoutSec      = 120
    }
    if ($null -ne $Corpo) {
        $json = $Corpo | ConvertTo-Json -Depth 10 -Compress
        $params["Body"] = [System.Text.Encoding]::UTF8.GetBytes($json)
        $params["ContentType"] = "application/json; charset=utf-8"
    }
    $status = 0
    $texto = ""
    try {
        $resp = Invoke-WebRequest @params
        $status = [int]$resp.StatusCode
        if ($resp.RawContentStream) {
            $texto = [System.Text.Encoding]::UTF8.GetString($resp.RawContentStream.ToArray())
        }
    } catch {
        $erro = $_
        $r = $erro.Exception.Response
        if ($null -eq $r) { throw }
        $status = [int]$r.StatusCode
        if ($erro.ErrorDetails -and $erro.ErrorDetails.Message) {
            $texto = $erro.ErrorDetails.Message
        } else {
            try {
                $stream = $r.GetResponseStream()
                $reader = New-Object System.IO.StreamReader($stream, [System.Text.Encoding]::UTF8)
                $texto = $reader.ReadToEnd()
            } catch {
                $texto = ""
            }
        }
    }
    $obj = $null
    if ($texto) {
        try { $obj = $texto | ConvertFrom-Json } catch { $obj = $null }
    }
    return [pscustomobject]@{ Status = $status; Json = $obj; Texto = $texto }
}

function Conferir {
    param(
        [string]$Nome,
        $Resposta,
        [int]$Esperado,
        [scriptblock]$Extra = $null
    )
    $passou = $Resposta.Status -eq $Esperado
    if ($passou -and $Extra) {
        try { $passou = [bool](& $Extra $Resposta.Json) } catch { $passou = $false }
    }
    if ($passou) {
        $script:ok++
        $script:linhas += "[OK]      $Nome -> HTTP $($Resposta.Status)"
    } else {
        $script:falhas++
        $resumo = if ($Resposta.Texto) { $Resposta.Texto.Substring(0, [Math]::Min(300, $Resposta.Texto.Length)) } else { "" }
        $script:linhas += "[FALHOU]  $Nome -> esperado $Esperado, veio $($Resposta.Status) $resumo"
    }
    Write-Host $script:linhas[-1]
}

Write-Host "Testando $BaseUrl" -ForegroundColor Cyan
$sufixo = Get-Random -Maximum 999999

$r = Chamar GET "/actuator/health"
Conferir "Health check publico" $r 200 { param($j) $j.status -eq "UP" }

$r = Chamar GET "/v3/api-docs"
Conferir "OpenAPI (Swagger) publicado" $r 200 { param($j) @($j.paths.PSObject.Properties).Count -gt 10 }

$r = Chamar GET "/api/usuarios/me"
Conferir "Rota protegida sem token" $r 401 { param($j) $j.codigo -eq "NAO_AUTENTICADO" }

$r = Chamar POST "/api/auth/login" -Corpo @{ email = "cida@confere.app"; senha = "Errada@123" }
Conferir "Login com senha errada" $r 401 { param($j) $j.codigo -eq "NAO_AUTENTICADO" }

$r = Chamar POST "/api/auth/login" -Corpo @{ email = "cida@confere.app"; senha = "Senha@123" }
Conferir "Login da cidada (cida)" $r 200 { param($j) $j.token -and $j.usuario.perfil -eq "CIDADAO" }
$tCida = $r.Json.token

$r = Chamar POST "/api/auth/login" -Corpo @{ email = "joao@confere.app"; senha = "Senha@123" }
Conferir "Login do guardiao (joao)" $r 200 { param($j) $j.token }
$tJoao = $r.Json.token

$r = Chamar POST "/api/auth/login" -Corpo @{ email = "admin@confere.app"; senha = "Senha@123" }
Conferir "Login do admin" $r 200 { param($j) $j.usuario.perfil -eq "ADMIN" }
$tAdmin = $r.Json.token

$r = Chamar POST "/api/auth/registrar" -Corpo @{ nome = "A"; email = "invalido"; senha = "123"; telefone = "999"; codIbgeMunicipio = 3550308 }
Conferir "Cadastro invalido (Bean Validation)" $r 400 { param($j) $j.codigo -eq "VALIDACAO" -and @($j.erros).Count -ge 3 }

$emailNovo = "teste$sufixo@confere.app"
$r = Chamar POST "/api/auth/registrar" -Corpo @{ nome = "Usuario de Teste"; email = $emailNovo; senha = "Senha@123"; telefone = "11987654321"; dtNascimento = "1960-05-10"; codIbgeMunicipio = 3550308 }
Conferir "Cadastro de novo cidadao" $r 201 { param($j) $j.id -gt 0 }

$r = Chamar POST "/api/auth/registrar" -Corpo @{ nome = "Usuario de Teste"; email = $emailNovo; senha = "Senha@123"; codIbgeMunicipio = 3550308 }
Conferir "Cadastro com e-mail repetido" $r 409 { param($j) $j.codigo -eq "CONFLITO" }

$r = Chamar POST "/api/auth/login" -Corpo @{ email = $emailNovo; senha = "Senha@123" }
Conferir "Login do novo cidadao" $r 200 { param($j) $j.token }
$tNovo = $r.Json.token

$r = Chamar GET "/api/usuarios/me" $tCida
Conferir "GET /usuarios/me" $r 200 { param($j) $j.email -eq "cida@confere.app" -and $j.municipio.codIbge -eq 3550308 }

$r = Chamar PUT "/api/usuarios/me" $tNovo @{ fonteGrande = $true; audioAtivo = $false }
Conferir "PUT /usuarios/me (acessibilidade)" $r 200 { param($j) $j.fonteGrande -eq $true -and $j.audioAtivo -eq $false }

$r = Chamar PUT "/api/usuarios/me/push-token" $tNovo @{ expoPushToken = "ExponentPushToken[teste$sufixo]" }
Conferir "PUT /usuarios/me/push-token" $r 204

$r = Chamar GET "/api/catalogos/canais" $tCida
Conferir "GET /catalogos/canais" $r 200 { param($j) @($j).Count -ge 6 }

$r = Chamar GET "/api/catalogos/tipos-golpe" $tCida
Conferir "GET /catalogos/tipos-golpe" $r 200 { param($j) @($j).Count -ge 10 }

$r = Chamar GET "/api/regras-offline" $tCida
Conferir "GET /regras-offline" $r 200 { param($j) @($j.regras).Count -ge 1 }
$versaoRegras = $r.Json.versao

$r = Chamar GET "/api/regras-offline?versao=$versaoRegras" $tCida
Conferir "GET /regras-offline com a mesma versao" $r 304

$verifGolpe = @{ tipoEntrada = "TEXTO"; conteudo = "INSS: sua prova de vida vence hoje. Envie uma selfie e o CPF 123.456.789-00 para nao perder o beneficio."; canal = "WHATSAPP"; modo = "ONLINE" }

$r = Chamar POST "/api/verificacoes" $tNovo $verifGolpe
Conferir "Verificacao sem consentimento (RN02)" $r 403 { param($j) $j.codigo -eq "CONSENTIMENTO_PENDENTE" }

$r = Chamar POST "/api/consentimentos" $tNovo @{ finalidade = "ANALISE_IA"; versaoTermo = "1.0"; aceito = $true }
Conferir "POST /consentimentos (ANALISE_IA)" $r 201 { param($j) $j.aceito -eq $true }

$r = Chamar GET "/api/consentimentos" $tNovo
Conferir "GET /consentimentos" $r 200 { param($j) @($j).Count -ge 1 }

$r = Chamar POST "/api/verificacoes" $tCida $verifGolpe
Conferir "POST /verificacoes (golpe, Spring AI)" $r 201 { param($j) $j.veredito -eq "GOLPE" -and @($j.frases).Count -ge 1 -and $j.conteudoMascarado -notmatch "123\.456\.789-00" }
$idVerif = $r.Json.id
if ($r.Json) { Write-Host "          frases: $(@($r.Json.frases) -join ' | ')" }

$r = Chamar GET "/api/verificacoes?pagina=0&tamanho=5" $tCida
Conferir "GET /verificacoes paginado" $r 200 { param($j) $j.tamanho -eq 5 -and $j.totalItens -ge 1 }

$r = Chamar GET "/api/verificacoes?veredito=GOLPE" $tCida
Conferir "GET /verificacoes filtrando por veredito" $r 200 { param($j) @($j.conteudo | Where-Object { $_.veredito -ne "GOLPE" }).Count -eq 0 }

$r = Chamar GET "/api/verificacoes?veredito=XYZ" $tCida
Conferir "Filtro com veredito invalido" $r 400 { param($j) $j.codigo -eq "VALIDACAO" }

$r = Chamar GET "/api/verificacoes/$idVerif" $tCida
Conferir "GET /verificacoes/{id}" $r 200 { param($j) $j.id -eq $idVerif }

$r = Chamar GET "/api/verificacoes/$idVerif" $tNovo
Conferir "Outro usuario nao ve a verificacao (RN09)" $r 404

$r = Chamar PUT "/api/verificacoes/$idVerif/feedback" $tCida @{ foiUtil = $true; desfecho = "NAO_PAGUEI"; valorEvitado = 1500.00 }
Conferir "PUT /verificacoes/{id}/feedback" $r 200 { param($j) $j.desfecho -eq "NAO_PAGUEI" }

$r = Chamar POST "/api/verificacoes" $tNovo @{ tipoEntrada = "TEXTO"; conteudo = "Mensagem recebida sem internet para conferir depois, urgente."; canal = "SMS"; modo = "OFFLINE" }
Conferir "POST /verificacoes OFFLINE (RN08)" $r 202 { param($j) $j.modo -eq "OFFLINE" }

$r = Chamar GET "/api/guardioes" $tCida
Conferir "GET /guardioes da cida" $r 200 { param($j) @($j | Where-Object { $_.status -eq "ATIVO" }).Count -ge 1 }

$r = Chamar POST "/api/guardioes" $tCida @{ emailGuardiao = "joao@confere.app"; parentesco = "Filho"; nivelMinimo = "GOLPE" }
Conferir "Convite duplicado (RN10)" $r 409 { param($j) $j.codigo -eq "GUARDIAO_DUPLICADO" }

$r = Chamar POST "/api/guardioes" $tNovo @{ emailGuardiao = $emailNovo; parentesco = "Eu mesmo" }
Conferir "Auto-convite bloqueado (RN10)" $r 400

$r = Chamar POST "/api/guardioes" $tNovo @{ emailGuardiao = "invalido"; parentesco = "" }
Conferir "Convite invalido (Bean Validation)" $r 400 { param($j) $j.codigo -eq "VALIDACAO" }

$r = Chamar POST "/api/guardioes" $tNovo @{ emailGuardiao = "joao@confere.app"; parentesco = "Neto"; nivelMinimo = "SUSPEITO" }
Conferir "POST /guardioes (convite)" $r 201 { param($j) $j.status -eq "PENDENTE" }
$idVinculo = $r.Json.id

$r = Chamar GET "/api/guardioes/convites" $tJoao
Conferir "GET /guardioes/convites do joao" $r 200 { param($j) @($j | Where-Object { $_.id -eq $idVinculo }).Count -eq 1 }

$r = Chamar POST "/api/guardioes/$idVinculo/aceitar" $tNovo
Conferir "Convidado errado tenta aceitar" $r 403

$r = Chamar POST "/api/guardioes/$idVinculo/aceitar" $tJoao
Conferir "POST /guardioes/{id}/aceitar" $r 200 { param($j) $j.status -eq "ATIVO" }

$r = Chamar GET "/api/protegidos" $tJoao
Conferir "GET /protegidos do joao" $r 200 { param($j) @($j).Count -ge 2 }

$r = Chamar PUT "/api/guardioes/$idVinculo" $tNovo @{ parentesco = "Sobrinho"; nivelMinimo = "GOLPE" }
Conferir "PUT /guardioes/{id}" $r 200 { param($j) $j.parentesco -eq "Sobrinho" }

$r = Chamar DELETE "/api/guardioes/$idVinculo" $tNovo
Conferir "DELETE /guardioes/{id} (revogar)" $r 204

$r = Chamar GET "/api/alertas?pagina=0&tamanho=10" $tJoao
Conferir "GET /alertas do guardiao (paginado)" $r 200 { param($j) $null -ne $j.totalItens }
$totalAlertas = $r.Json.totalItens
Write-Host "          alertas do joao: $totalAlertas (so aparecem com o RabbitMQ e os consumidores ligados)"
if ($totalAlertas -gt 0) {
    $idAlerta = @($r.Json.conteudo)[0].id
    $r = Chamar PATCH "/api/alertas/$idAlerta" $tJoao @{ status = "LIDO" }
    Conferir "PATCH /alertas/{id} (LIDO)" $r 200 { param($j) $j.status -eq "LIDO" }
}

$r = Chamar DELETE "/api/verificacoes/$idVerif" $tCida
Conferir "DELETE /verificacoes/{id}" $r 204

$r = Chamar GET "/api/verificacoes/$idVerif" $tCida
Conferir "Verificacao excluida nao existe mais" $r 404 { param($j) $j.codigo -eq "NAO_ENCONTRADO" }

$novoTipo = @{ codigo = "GOLPE_TESTE_$sufixo"; nome = "Golpe de teste"; descricao = "Criado pelo script de testes"; gravidade = 3; orgaoImitado = "Correios" }

$r = Chamar POST "/api/admin/tipos-golpe" $tCida $novoTipo
Conferir "Cidadao no CRUD admin (papel)" $r 403 { param($j) $j.codigo -eq "SEM_PERMISSAO" }

$r = Chamar POST "/api/admin/tipos-golpe" $tAdmin $novoTipo
Conferir "POST /admin/tipos-golpe" $r 201
$idTipo = $r.Json.id

$r = Chamar GET "/api/admin/tipos-golpe?pagina=0&tamanho=5" $tAdmin
Conferir "GET /admin/tipos-golpe paginado" $r 200 { param($j) $j.tamanho -eq 5 -and $j.totalItens -ge 11 }

$r = Chamar GET "/api/admin/tipos-golpe/$idTipo" $tAdmin
Conferir "GET /admin/tipos-golpe/{id}" $r 200

$novoTipo.nome = "Golpe de teste editado"
$r = Chamar PUT "/api/admin/tipos-golpe/$idTipo" $tAdmin $novoTipo
Conferir "PUT /admin/tipos-golpe/{id}" $r 200 { param($j) $j.nome -eq "Golpe de teste editado" }

$r = Chamar DELETE "/api/admin/tipos-golpe/$idTipo" $tAdmin
Conferir "DELETE /admin/tipos-golpe/{id}" $r 204

$r = Chamar GET "/api/admin/tipos-golpe/$idTipo" $tAdmin
Conferir "Tipo excluido nao existe mais" $r 404

$novaRegra = @{ padrao = "(?i)teste$sufixo"; tipoSinal = "URGENCIA"; peso = 0.30; mensagem = "Regra criada pelo script de testes"; ativo = $true }

$r = Chamar POST "/api/admin/regras-offline" $tAdmin $novaRegra
Conferir "POST /admin/regras-offline" $r 201
$idRegra = $r.Json.id

$r = Chamar GET "/api/admin/regras-offline?pagina=0&tamanho=5" $tAdmin
Conferir "GET /admin/regras-offline paginado" $r 200

$novaRegra.mensagem = "Regra editada pelo script de testes"
$r = Chamar PUT "/api/admin/regras-offline/$idRegra" $tAdmin $novaRegra
Conferir "PUT /admin/regras-offline/{id}" $r 200 { param($j) $j.mensagem -eq "Regra editada pelo script de testes" }

$r = Chamar DELETE "/api/admin/regras-offline/$idRegra" $tAdmin
Conferir "DELETE /admin/regras-offline/{id}" $r 204

$r = Chamar GET "/api/rota-que-nao-existe" $tCida
Conferir "Rota inexistente (ProblemDetail)" $r 404 { param($j) $j.codigo -eq "NAO_ENCONTRADO" }

Write-Host ""
$cor = if ($script:falhas -eq 0) { "Green" } else { "Red" }
Write-Host "Resultado: $($script:ok) OK, $($script:falhas) falhas" -ForegroundColor $cor
