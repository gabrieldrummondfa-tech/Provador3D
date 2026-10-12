# Instruções para o Claude instalar o Provador 3D neste PC

Objetivo: deixar o Provador 3D (editor de corpo + gerador de fotos) funcionando no ComfyUI deste PC Windows
e abrir o app em `/provador`, pronto para clicar em "Gerar foto".

## Passos

1. **ComfyUI.** Verifique se já existe uma pasta do ComfyUI (a que contém `custom_nodes` e `models`;
   na versão Desktop costuma ficar em `Documentos\ComfyUI`).
   Se não existir, baixe o instalador oficial em https://comfy.org/download, instale e abra o ComfyUI uma vez.
   O PC tem placa AMD Radeon: escolha a opção AMD/ROCm quando o instalador perguntar.
   Confirmações do Windows (UAC, firewall) ficam para o usuário aprovar.

2. **Extensão + modelos.** Rode no PowerShell, a partir de uma cópia deste repositório:

   ```powershell
   powershell -ExecutionPolicy Bypass -File instalar.ps1
   ```

   Se o script não achar o ComfyUI sozinho, passe a pasta: `-ComfyRoot "C:\caminho\ComfyUI"`.
   O script clona a extensão para `custom_nodes\Provador3D` e baixa os dois modelos (cerca de 9,5 GB)
   para `models\checkpoints` e `models\controlnet`, retomando de onde parou se a conexão cair.
   Para pular os modelos e baixá-los pelo próprio app: acrescente `-SemModelos`.

   Sem PowerShell disponível, faça à mão: `git clone https://github.com/gabrieldrummondfa-tech/Provador3D`
   dentro de `custom_nodes`, depois use o botão "Baixar modelos automaticamente" na aba Foto do app.

3. **Reinicie o ComfyUI.** No log deve aparecer `[Provador 3D] app disponível em /provador`.

4. **Abra o app** no navegador: endereço do ComfyUI + `/provador`
   (Desktop: normalmente `http://127.0.0.1:8000/provador`; portátil: `http://127.0.0.1:8188/provador`).
   Na aba **Foto**, o quadro do topo deve ficar verde (modelo de foto e guia do corpo encontrados).

5. **Teste:** clique em **Gerar foto** com a qualidade "Rápida". A primeira geração demora mais (carrega o modelo).
   Se der erro, a mensagem aparece na tela e o detalhe fica na janela/log do ComfyUI.

## Se algo falhar

- "Não encontrei o ComfyUI" no app: o app precisa ser aberto pelo endereço do ComfyUI terminado em `/provador`.
- Repositório privado e clone sem login: faça login no GitHub no Git do Windows ou torne o repositório público.
- Erro de memória de vídeo na geração: use qualidade "Rápida" e 1 foto por vez.
