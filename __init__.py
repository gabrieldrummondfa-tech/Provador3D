"""Provador 3D: editor de corpo + gerador de fotos dentro do ComfyUI.

Abra http://ENDERECO-DO-COMFYUI/provador no navegador.
Base humana: MakeHuman (CC0). Renderização 3D: three.js (MIT).
"""
import os
import threading

from aiohttp import web
import folder_paths
import server

APP_DIR = os.path.join(os.path.dirname(os.path.realpath(__file__)), "app")
_routes = server.PromptServer.instance.routes

# Modelos que o app usa. O download automático grava direto na pasta de modelos do ComfyUI.
MODELS = [
    {
        "id": "foto",
        "label": "Modelo de foto (RealVisXL V5)",
        "folder": "checkpoints",
        "file": "RealVisXL_V5.0_fp16.safetensors",
        "url": "https://huggingface.co/SG161222/RealVisXL_V5.0/resolve/main/RealVisXL_V5.0_fp16.safetensors",
        "size": 6_938_000_000,
    },
    {
        "id": "corpo",
        "label": "Guia do corpo (ControlNet de profundidade)",
        "folder": "controlnet",
        "file": "controlnet-depth-sdxl-1.0.safetensors",
        "url": "https://huggingface.co/diffusers/controlnet-depth-sdxl-1.0/resolve/main/diffusion_pytorch_model.fp16.safetensors",
        "size": 2_502_000_000,
    },
]
_MIRROR = os.environ.get("PROVADOR_MIRROR")  # só para testes
_state = {m["id"]: {"status": "idle", "done": 0, "total": m["size"], "error": ""} for m in MODELS}
_lock = threading.Lock()
_worker = None


def _target(m):
    return os.path.join(folder_paths.get_folder_paths(m["folder"])[0], m["file"])


def _present(m):
    try:
        return m["file"] in folder_paths.get_filename_list(m["folder"]) or os.path.isfile(_target(m))
    except Exception:
        return os.path.isfile(_target(m))


def _download_all():
    import requests

    for m in MODELS:
        st = _state[m["id"]]
        if _present(m):
            st.update(status="ok", done=st["total"])
            continue
        dest = _target(m)
        part = dest + ".part"
        url = m["url"].replace("https://huggingface.co", _MIRROR) if _MIRROR else m["url"]
        try:
            os.makedirs(os.path.dirname(dest), exist_ok=True)
            have = os.path.getsize(part) if os.path.exists(part) else 0
            headers = {"Range": f"bytes={have}-"} if have else {}
            with requests.get(url, stream=True, timeout=60, headers=headers, allow_redirects=True) as r:
                if r.status_code not in (200, 206):
                    raise RuntimeError(f"o servidor respondeu {r.status_code}")
                if r.status_code == 200:
                    have = 0
                total = int(r.headers.get("Content-Length", 0)) + have
                st.update(status="downloading", done=have, total=total or m["size"], error="")
                with open(part, "ab" if have else "wb") as f:
                    for chunk in r.iter_content(chunk_size=4 * 1024 * 1024):
                        if chunk:
                            f.write(chunk)
                            st["done"] += len(chunk)
            os.replace(part, dest)
            st.update(status="ok", done=st["total"])
        except Exception as e:  # mantém o .part para retomar depois
            st.update(status="error", error=str(e)[:200])


@_routes.get("/provador")
async def provador_redirect(request):
    raise web.HTTPFound("/provador/")


@_routes.get("/provador/")
async def provador_index(request):
    return web.FileResponse(os.path.join(APP_DIR, "index.html"), headers={"Cache-Control": "no-cache"})


@_routes.get("/provador/api/models")
async def provador_models(request):
    out = []
    for m in MODELS:
        st = dict(_state[m["id"]])
        if st["status"] in ("idle", "error") and _present(m):
            st["status"] = "ok"
        out.append({"id": m["id"], "label": m["label"], "file": m["file"], "folder": m["folder"], **st})
    running = _worker is not None and _worker.is_alive()
    return web.json_response({"models": out, "running": running})


@_routes.post("/provador/api/download")
async def provador_download(request):
    global _worker
    with _lock:
        if _worker is None or not _worker.is_alive():
            _worker = threading.Thread(target=_download_all, name="provador-download", daemon=True)
            _worker.start()
    return web.json_response({"started": True})


_routes.static("/provador/", APP_DIR)

NODE_CLASS_MAPPINGS = {}
NODE_DISPLAY_NAME_MAPPINGS = {}
print("[Provador 3D] app disponível em /provador")
