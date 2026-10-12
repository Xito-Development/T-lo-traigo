# CLAUDE.md — Guía operativa para el agente (proyecto Fisiory)

> Manual de trabajo para Claude Code. **No duplica el brief**: la especificación
> completa está en `docs/brief_implementacion_fisio.md`. Este fichero dice **cómo**
> trabajar de forma eficiente; el brief dice **qué** construir.

## Documentos del proyecto (leer UNA sola vez)
- `docs/brief_implementacion_fisio.md` — especificación (arquitectura, módulos A–H, §7).
- `config.yaml` — parámetros de runtime.
- `reference/SKILL.md` — especificación del **módulo F** (analizador) y límites clínicos.

## Reglas de eficiencia (tokens y recursos) — OBLIGATORIO
1. **Lectura única:** al iniciar la sesión, lee el brief, `config.yaml` y `reference/SKILL.md`
   **una vez** y construye un índice mental. **No los releas** salvo que necesites una sección
   concreta; en ese caso usa `grep`/lectura por rango, nunca el fichero completo.
2. **No releas ficheros ya en contexto.** Si algo ya se leyó en esta sesión, reutilízalo.
3. **Lectura dirigida:** usa `grep`/`rg` y lectura por líneas en vez de abrir archivos enteros.
4. **Trabaja por módulo, no por repo:** carga solo los ficheros del módulo actual. No escanees
   todo el árbol en cada paso.
5. **Diffs y respuestas mínimas:** no expliques de más; muestra el cambio y el resultado del test.
6. **Sin tareas redundantes:** no repitas instalaciones, builds ni búsquedas ya hechas.
7. **Subagentes solo si ahorran contexto** (p. ej. una exploración aislada que no debe contaminar
   el contexto principal). Si no ahorran, no los uses.

## Skills / plugins — buscar antes de implementar
- Al empezar, **comprueba las skills/plugins disponibles** (marketplace de Claude Code y skills
  instaladas) relevantes para: scaffolding de proyecto Python, **pytest**, manejo de audio
  (faster-whisper / pyannote), empaquetado y linters. **Si existe una skill adecuada, úsala**
  en vez de reescribir desde cero.
- **Módulo F:** `reference/SKILL.md` (rehabilitation-analyzer) es su especificación. Si puede
  instalarse/invocarse como skill, úsala; si no, impleméntala siguiendo ese documento.
- No inventes skills: si no existe una adecuada, impleméntalo a mano y sigue.

## Convenciones
- **Python 3.11**, `venv` en `.venv/`. Tipado y `pydantic` para esquemas.
- Estructura de repo según §10 del brief (`src/`, `web/`, `data/`, `tests/`).
- Secretos por **variable de entorno** (`HF_TOKEN`), nunca en el código ni en git.
- `data/` y `.venv/` en `.gitignore`.

## Entorno (YA RESUELTO — no reinvestigar)
- **Python 3.11 obligatorio.** NO usar 3.12+ ni 3.14: PyTorch no tiene wheels para ellas.
  Crear el venv con `py -3.11 -m venv .venv`.
- **torch/torchaudio ≥ 2.8 desde el índice CUDA `cu128`** (cu124 solo llega a 2.6 y rompe
  pyannote 4.x, que exige ≥2.8). Instalarlos **DESPUÉS** de `requirements.txt`, porque PyPI
  reinstala la build CPU y la pisa.
- Verificación obligatoria tras instalar: `torch.cuda.is_available()` debe ser `True`.
- En Windows, ejecutar siempre en **Git Bash** (Claude Code lo requiere).

## Comandos del proyecto
```bash
py -3.11 -m venv .venv
source .venv/Scripts/activate                 # PowerShell: .\.venv\Scripts\Activate.ps1
python -m pip install --upgrade pip
pip install -r requirements.txt
# torch/torchaudio CUDA SIEMPRE al final, desde cu128:
pip install torch torchaudio --index-url https://download.pytorch.org/whl/cu128
python -c "import torch; print(torch.__version__, torch.cuda.is_available())"  # → +cu128 True
pytest -q                       # tests
python -m src.orchestrator      # ejecutar pipeline (cuando exista)
```

## Restricciones que NO se negocian
- **VRAM 8 GB → orquestación SECUENCIAL** (§5): nunca ASR-grande + LLM en GPU a la vez;
  pyannote en CPU. Modelos limitados a 4B / Whisper distil-turbo.
- **Límites clínicos** (§8 / SKILL.md): no diagnosticar, prescribir ni pronosticar. Solo extraer.
- **RGPD:** todo local; `store_audio: false` por defecto; sin datos identificativos.

## Orden de trabajo y "definición de hecho"
Implementa **A → B → C → D → E → F → G → H** (§7 del brief). Para CADA módulo:
1. Implementa en `src/`.
2. Escribe un test mínimo en `tests/`.
3. Verifica el **criterio de aceptación** del módulo (está en §7).
4. `pytest -q` en verde.
5. `git commit` con mensaje claro (`feat(modulo-X): ...`).
6. **Detente y pide validación** antes del siguiente módulo.

## Estado inicial esperado
Ya existen: `config.yaml` y `src/extract.py` (módulo E base). Empieza por el **módulo A**
(captura desde micro integrado) y respeta lo ya hecho en E al llegar a él.
