# Usar Claude Code desde GitHub

1. En tu PC, con Claude Code instalado, abre el repositorio y escribe `/install-github-app`: instala la app de Claude en
   `Xito-Development/T-lo-traigo` y guarda el secreto necesario (te lo hace todo el asistente).
   - Alternativa manual: instala la app desde github.com/apps/claude y añade en GitHub → Settings → Secrets → Actions
     `ANTHROPIC_API_KEY` (clave de console.anthropic.com) o `CLAUDE_CODE_OAUTH_TOKEN` (`claude setup-token`).
2. Abre una incidencia (Issues → New) describiendo la tarea y escribe `@claude` en ella. Claude responde, hace los cambios
   y abre un PR. Revísalo y acéptalo (Merge): al llegar a `main` se compila y se publica como siempre.
3. El contexto del proyecto está en `CLAUDE.md`.
