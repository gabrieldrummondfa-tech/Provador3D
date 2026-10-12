# Provador 3D - instalador para Windows
# Instala a extensão no ComfyUI e baixa os dois modelos (cerca de 9,5 GB).
# Uso:  powershell -ExecutionPolicy Bypass -File instalar.ps1 [-ComfyRoot "C:\caminho\ComfyUI"] [-SemModelos]
# Compatível com o Windows PowerShell 5.1 que já vem no Windows.

param(
    [string]$ComfyRoot = "",
    [switch]$SemModelos
)

$ErrorActionPreference = "Stop"
$Repo = "https://github.com/gabrieldrummondfa-tech/Provador3D"
$Models = @(
    @{ Pasta = "checkpoints"; Arquivo = "RealVisXL_V5.0_fp16.safetensors";
       Url = "https://huggingface.co/SG161222/RealVisXL_V5.0/resolve/main/RealVisXL_V5.0_fp16.safetensors" },
    @{ Pasta = "controlnet"; Arquivo = "controlnet-depth-sdxl-1.0.safetensors";
       Url = "https://huggingface.co/diffusers/controlnet-depth-sdxl-1.0/resolve/main/diffusion_pytorch_model.fp16.safetensors" }
)

function Passo($texto) { Write-Host ""; Write-Host "==> $texto" -ForegroundColor Yellow }

function Test-ComfyRoot($dir) {
    if (-not $dir) { return $false }
    return (Test-Path (Join-Path $dir "custom_nodes")) -and (Test-Path (Join-Path $dir "models"))
}

function Find-ComfyRoot {
    $home_ = $env:USERPROFILE
    $candidatos = @(
        (Join-Path $home_ "Documents\ComfyUI"),
        (Join-Path $home_ "OneDrive\Documents\ComfyUI"),
        (Join-Path $home_ "OneDrive\Documentos\ComfyUI"),
        (Join-Path $home_ "ComfyUI"),
        (Join-Path $home_ "Desktop\ComfyUI_windows_portable\ComfyUI"),
        (Join-Path $home_ "Downloads\ComfyUI_windows_portable\ComfyUI"),
        "C:\ComfyUI_windows_portable\ComfyUI",
        "C:\ComfyUI"
    )
    foreach ($c in $candidatos) { if (Test-ComfyRoot $c) { return $c } }
    # busca mais ampla (rasa) na pasta do usuário e na raiz do C:
    foreach ($base in @($home_, "C:\")) {
        $achado = Get-ChildItem -Path $base -Directory -Recurse -Depth 3 -Filter "custom_nodes" -ErrorAction SilentlyContinue |
            Where-Object { Test-Path (Join-Path $_.Parent.FullName "models") } | Select-Object -First 1
        if ($achado) { return $achado.Parent.FullName }
    }
    return ""
}

Passo "Procurando o ComfyUI"
if (-not (Test-ComfyRoot $ComfyRoot)) { $ComfyRoot = Find-ComfyRoot }
if (-not $ComfyRoot) {
    Write-Host "Não encontrei a pasta do ComfyUI (a que tem 'custom_nodes' e 'models')." -ForegroundColor Red
    Write-Host "Instale o ComfyUI (comfy.org/download), abra uma vez e rode de novo, ou informe a pasta:"
    Write-Host '  powershell -ExecutionPolicy Bypass -File instalar.ps1 -ComfyRoot "C:\caminho\ComfyUI"'
    exit 1
}
Write-Host "ComfyUI: $ComfyRoot"

Passo "Instalando a extensão Provador 3D"
$destino = Join-Path $ComfyRoot "custom_nodes\Provador3D"
$git = Get-Command git -ErrorAction SilentlyContinue
if (Test-Path (Join-Path $destino ".git")) {
    if ($git) { git -C $destino pull --ff-only; Write-Host "Atualizado." } else { Write-Host "Já instalado (sem git para atualizar)." }
} elseif (Test-Path (Join-Path $destino "__init__.py")) {
    Write-Host "Já instalado."
} elseif ($git) {
    git clone --depth 1 $Repo $destino
    if ($LASTEXITCODE -ne 0) { throw "O git clone falhou. Se o repositório for privado, faça login no GitHub ou torne-o público." }
} else {
    $zip = Join-Path $env:TEMP "provador3d.zip"
    $tmp = Join-Path $env:TEMP "provador3d_unzip"
    try {
        Invoke-WebRequest -Uri "$Repo/archive/refs/heads/main.zip" -OutFile $zip -UseBasicParsing
    } catch {
        throw "Não consegui baixar o repositório. Se ele for privado, instale o Git (git-scm.com) e faça login, ou torne-o público."
    }
    if (Test-Path $tmp) { Remove-Item $tmp -Recurse -Force }
    Expand-Archive -Path $zip -DestinationPath $tmp -Force
    $pasta = Get-ChildItem $tmp -Directory | Select-Object -First 1
    Move-Item $pasta.FullName $destino
    Remove-Item $zip -Force
}
Write-Host "Extensão em: $destino"

if (-not $SemModelos) {
    Passo "Baixando os modelos (pode demorar; se cair, rode de novo que continua de onde parou)"
    foreach ($m in $Models) {
        $dir = Join-Path $ComfyRoot ("models\" + $m.Pasta)
        New-Item -ItemType Directory -Force -Path $dir | Out-Null
        $alvo = Join-Path $dir $m.Arquivo
        if (Test-Path $alvo) { Write-Host "Já existe: $($m.Arquivo)"; continue }
        $parte = "$alvo.part"
        Write-Host "Baixando $($m.Arquivo) ..."
        & curl.exe -L --fail --retry 5 --retry-delay 5 -C - -o $parte $m.Url
        if ($LASTEXITCODE -ne 0) { throw "Falha ao baixar $($m.Arquivo). Rode o instalador de novo para continuar." }
        Move-Item $parte $alvo -Force
    }
}

Passo "Pronto"
Write-Host "1. Reinicie o ComfyUI."
Write-Host "2. Abra no navegador o endereço do ComfyUI com /provador no final."
Write-Host "   Desktop: http://127.0.0.1:8000/provador    Portátil: http://127.0.0.1:8188/provador"
