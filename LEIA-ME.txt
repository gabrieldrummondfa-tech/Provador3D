PROVADOR 3D — editor de corpo + gerador de fotos (roda no seu PC, sem créditos)

O que é
  Uma extensão do ComfyUI. Ela adiciona o app em  /provador :
  você edita a personagem e toca em "Gerar foto". O ComfyUI gera a foto no seu PC
  seguindo o corpo que você editou. As fotos ficam em  ComfyUI/output/provador.

1) Instale o ComfyUI
   comfy.org/download  (instalador oficial; em placas AMD no Windows usa ROCm, ainda em beta)
   Abra o ComfyUI uma vez para confirmar que funciona.

2) Baixe os dois modelos (cerca de 9,5 GB no total)
   a) Modelo de foto:
      huggingface.co/SG161222/RealVisXL_V5.0  ->  RealVisXL_V5.0_fp16.safetensors
      coloque em:  ComfyUI/models/checkpoints/
   b) Guia do corpo (ControlNet de profundidade):
      huggingface.co/diffusers/controlnet-depth-sdxl-1.0  ->  diffusion_pytorch_model.fp16.safetensors
      renomeie para  controlnet-depth-sdxl-1.0.safetensors
      coloque em:  ComfyUI/models/controlnet/

3) Instale o Provador
   Copie a pasta  ComfyUI-Provador3D  (esta pasta inteira) para  ComfyUI/custom_nodes/
   Na versão Desktop, a pasta ComfyUI fica onde você escolheu na instalação
   (normalmente Documentos\ComfyUI).
   Reinicie o ComfyUI. Na janela de log deve aparecer:  [Provador 3D] app disponível em /provador

4) Abra o app no PC
   Use o mesmo endereço do ComfyUI e acrescente /provador. Exemplos:
     versão portátil:  http://127.0.0.1:8188/provador
     versão Desktop:   http://127.0.0.1:8000/provador   (confira a porta na barra de endereço do ComfyUI)

5) Abrir no celular (mesmo Wi-Fi de casa)
   O ComfyUI precisa aceitar conexões da rede:
     versão portátil: edite o arquivo .bat que você usa para abrir o ComfyUI e acrescente  --listen
                      no fim da linha que chama main.py
     versão Desktop:  em Configurações, procure "Server-Config" e mude o endereço de escuta para 0.0.0.0
   No PC, rode  ipconfig  no Prompt de Comando e anote o "Endereço IPv4" (ex.: 192.168.0.15).
   No celular, abra:  http://192.168.0.15:8188/provador   (troque pelo seu IP e porta)
   Se o Windows perguntar sobre o firewall, permita em "redes privadas".
   Atenção: com isso, qualquer aparelho no mesmo Wi-Fi consegue abrir o ComfyUI. Use só na rede de casa.

Dicas
  - A primeira foto demora mais: o modelo é carregado na memória.
  - "Qualidade Alta" faz duas etapas (mais detalhe de pele e cabelo) e leva mais tempo.
  - "Força do corpo editado": 0,5 a 0,75. Mais alto segue o editor; mais baixo deixa a foto mais natural.
  - Para mudar só a foto sem mudar o corpo, toque em "Gerar outra": a semente muda a cada vez.

Créditos
  Base humana: MakeHuman (CC0). 3D no navegador: three.js (licença MIT, em app/vendor/three/LICENSE).
