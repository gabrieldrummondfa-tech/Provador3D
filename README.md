PROVADOR 3D — editor de corpo + gerador de fotos (roda no seu PC, sem créditos)

O que é
  Uma extensão do ComfyUI. Ela adiciona o app em  /provador :
  você edita a personagem e clica em "Gerar foto". O ComfyUI gera a foto no seu PC
  seguindo o corpo que você editou. As fotos ficam em  ComfyUI/output/provador.

Instalação (3 passos)
  1) Instale o ComfyUI: comfy.org/download (em placas AMD no Windows usa ROCm, ainda em beta).
  2) No ComfyUI, abra o Manager > "Install via Git URL" e cole:
        https://github.com/gabrieldrummondfa-tech/Provador3D
     Sem o Manager: dentro da pasta ComfyUI\custom_nodes rode
        git clone https://github.com/gabrieldrummondfa-tech/Provador3D
     Reinicie o ComfyUI.
  3) Abra no navegador o endereço do ComfyUI com /provador no final, por exemplo
        http://127.0.0.1:8000/provador   (Desktop)   ou   http://127.0.0.1:8188/provador   (portátil)
     Na aba Foto, clique em "Baixar modelos automaticamente". O app baixa os dois modelos
     (cerca de 9,5 GB) para as pastas certas. Se a internet cair, clique de novo: continua de onde parou.

NO CELULAR (sem PC): app Android
  1) Baixe o Provador3D.apk em github.com/gabrieldrummondfa-tech/Provador3D/releases (pelo celular)
     e instale. O Android vai pedir para permitir "instalar apps desconhecidos".
  2) Instale o Local Dream (github.com/xororz/local-dream/releases), o motor que gera a foto
     no próprio celular. Nele, baixe um modelo realista e carregue (abra a tela de gerar).
     SDXL roda no chip de IA dos Snapdragon 8 Gen 3 em diante; SD 1.5 roda em aparelhos mais simples.
  3) Abra o Provador 3D, ajuste a personagem e, na aba Foto, toque em Gerar foto.
     As fotos vão para a galeria, em Imagens/Provador3D.
  O Local Dream é de uso pessoal e não comercial (licença CC BY-NC 4.0).

Atualizar
  Manager > Update no Provador3D, ou  git pull  dentro da pasta da extensão.

Dicas
  - A primeira foto demora mais: o modelo é carregado na memória.
  - "Qualidade Alta" faz duas etapas (mais detalhe de pele e cabelo) e leva mais tempo.
  - "Força do corpo editado": 0,5 a 0,75. Mais alto segue o editor; mais baixo deixa a foto mais natural.
  - "Gerar outra" muda só a foto, sem mudar o corpo.

Modelos usados
  RealVisXL V5.0 (huggingface.co/SG161222/RealVisXL_V5.0)
  ControlNet Depth SDXL (huggingface.co/diffusers/controlnet-depth-sdxl-1.0)

Créditos
  Base humana: MakeHuman (CC0). 3D no navegador: three.js (licença MIT, em app/vendor/three/LICENSE).
