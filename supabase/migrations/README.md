# Migraciones

Los archivos `20261008*` y `20261012000005*` reproducen el esquema base. Las migraciones posteriores
(v0.3–v0.4) se aplicaron directamente en el proyecto `qqqudthqgpojbipsevna`:

- `role_superadmin`, `v03_admin_reviews_schedule_penalties`, `cron_stale_penalties`
- `debug_tools`, `debug_tools_superadmin`, `account_deletion`

Para traer el esquema completo al repositorio:
```bash
supabase link --project-ref qqqudthqgpojbipsevna
supabase db pull
```
