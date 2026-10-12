"""Provador 3D: editor de corpo + gerador de fotos dentro do ComfyUI.

Abra http://ENDERECO-DO-COMFYUI/provador no navegador (no PC ou no celular, na mesma rede).
Base humana: MakeHuman (CC0). Renderização 3D: three.js (MIT).
"""
import os
from aiohttp import web
import server

APP_DIR = os.path.join(os.path.dirname(os.path.realpath(__file__)), "app")
_routes = server.PromptServer.instance.routes


@_routes.get("/provador")
async def provador_redirect(request):
    raise web.HTTPFound("/provador/")


@_routes.get("/provador/")
async def provador_index(request):
    return web.FileResponse(os.path.join(APP_DIR, "index.html"), headers={"Cache-Control": "no-cache"})


_routes.static("/provador/", APP_DIR)

NODE_CLASS_MAPPINGS = {}
NODE_DISPLAY_NAME_MAPPINGS = {}
print("[Provador 3D] app disponível em /provador")
