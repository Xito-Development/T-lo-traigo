-- Auditoría v0.6.3: permisos de la entidad, reglas de reparto, conservación de datos y rendimiento.

-- 1) El administrador de la entidad ve los tickets, el chat y las reclamaciones de SUS pedidos
drop policy if exists ticket_read on storage.objects;
create policy ticket_read on storage.objects for select to authenticated using (bucket_id = 'tickets' and ((select public.is_admin())
  or exists(select 1 from public.orders o where o.id::text = (storage.foldername(name))[1]
            and (o.driver_id = (select auth.uid()) or o.placed_by = (select auth.uid()) or public.can_act_for(o.customer_id)
                 or (o.org_id is not null and public.is_gestor(o.org_id))))));
drop policy if exists msg_read on public.order_messages;
create policy msg_read on public.order_messages for select to authenticated using (
  exists(select 1 from public.orders o where o.id = order_id and (o.driver_id = (select auth.uid()) or o.placed_by = (select auth.uid())
    or public.can_act_for(o.customer_id) or (select public.is_admin()) or (o.org_id is not null and public.is_gestor(o.org_id)))));
drop policy if exists cmp_read on public.complaints;
create policy cmp_read on public.complaints for select to authenticated using (user_id = (select auth.uid()) or (select public.is_admin())
  or exists(select 1 from public.orders o where o.id = order_id and o.org_id is not null and public.is_gestor(o.org_id)));
create or replace function public.answer_complaint(p_id bigint, p_response text) returns void language sql security definer set search_path = public as $$
  update public.complaints c set response = p_response, status = 'respondida', responded_at = now()
  where c.id = p_id and (public.is_admin() or exists(select 1 from public.orders o where o.id = c.order_id and o.org_id is not null and public.is_gestor(o.org_id))) $$;

-- El gestor también puede escribir en el chat de los pedidos de su entidad
create or replace function public.send_message(p_order bigint, p_body text) returns void
language plpgsql security definer set search_path = public as $$
declare o public.orders; r text;
begin
  select * into o from public.orders where id = p_order;
  if o.id is null then raise exception 'Pedido no encontrado'; end if;
  if o.driver_id = auth.uid() then r := 'repartidor';
  elsif o.placed_by = auth.uid() or public.can_act_for(o.customer_id) then r := 'cliente';
  elsif public.is_admin() or (o.org_id is not null and public.is_gestor(o.org_id)) then r := 'admin';
  else raise exception 'No autorizado'; end if;
  if o.status = 'cancelado' or (o.status = 'entregado' and o.updated_at < now() - interval '24 hours') then raise exception 'El chat de este pedido está cerrado'; end if;
  insert into public.order_messages(order_id, sender_id, sender_role, body) values (p_order, auth.uid(), r, left(trim(p_body), 1000));
end $$;

-- 2) Ticket siempre obligatorio (es la base de la trazabilidad) y no pedir para días sin reparto
update public.organizations set require_ticket = true;
create or replace function public.check_org_rules() returns trigger language plpgsql security definer set search_path = public as $$
declare o public.organizations;
begin
  select * into o from public.organizations where id = new.org_id;
  if o.id is null then return new; end if;
  if not o.active then raise exception 'Este servicio no está disponible ahora en tu pueblo'; end if;
  if not o.accepting_orders then raise exception '%', coalesce(nullif(o.welcome_message, ''), 'Ahora mismo no se aceptan pedidos. Inténtalo más tarde.'); end if;
  if new.payment_method = 'efectivo' and not o.allow_cash and o.products_payment <> 'entidad' then raise exception 'Este servicio no acepta pago en efectivo'; end if;
  if new.payment_method = 'bizum' and not (o.allow_bizum_driver or (o.allow_bizum_org and coalesce(o.bizum_org_phone, '') <> '')) then raise exception 'Este servicio no acepta Bizum'; end if;
  if new.scheduled_date is not null and not (extract(isodow from new.scheduled_date)::int = any(o.delivery_days)) then
    raise exception 'Ese día no hay reparto en tu pueblo. Elige otro día.'; end if;
  return new;
end $$;

-- 3) Conservación de datos (minimización, art. 5.1.e RGPD): chat 12 meses
select cron.schedule('limpiar-chat', '30 3 * * 0', $$delete from public.order_messages where created_at < now() - interval '12 months'$$);

-- 4) Rendimiento: índices de claves ajenas sin índice
create index if not exists org_invites_created_by_idx on public.org_invites(created_by);
create index if not exists refund_requests_order_status_idx on public.refund_requests(status);
create index if not exists orders_status_org_idx on public.orders(org_id, status);
create index if not exists orders_created_idx on public.orders(created_at);

-- 5) Imagen de marca de cada entidad (color y logo para sus usuarios)
alter table public.organizations add column if not exists brand_color text check (brand_color is null or brand_color ~ '^#[0-9A-Fa-f]{6}$'),
  add column if not exists logo_url text check (logo_url is null or logo_url ~ '^https://');
do $$ declare f text; begin
  select pg_get_functiondef('public.update_organization(bigint,jsonb)'::regprocedure) into f;
  if f not like '%brand_color%' then
    f := replace(f, 'products_payment = coalesce(p->>''products_payment'', products_payment),',
      'products_payment = coalesce(p->>''products_payment'', products_payment),
    brand_color = case when p ? ''brand_color'' then nullif(p->>''brand_color'', '''') else brand_color end,
    logo_url = case when p ? ''logo_url'' then nullif(p->>''logo_url'', '''') else logo_url end,');
    execute f;
  end if;
end $$;

-- 6) Avisos para la entidad: el administrador recibe las notificaciones del chat de sus pedidos (reclamaciones, cambios…)
create or replace function public.unread_messages() returns jsonb language sql stable security definer set search_path = public as $$
  select coalesce(jsonb_agg(jsonb_build_object('order_id', m.order_id, 'id', m.id, 'body', m.body, 'role', m.sender_role) order by m.id), '[]')
  from public.order_messages m join public.orders o on o.id = m.order_id
  where m.read_at is null and m.sender_id is distinct from auth.uid() and m.created_at > now() - interval '2 days'
    and (o.driver_id = auth.uid() or o.placed_by = auth.uid() or public.can_act_for(o.customer_id)
         or (m.sender_role = 'sistema' and o.org_id is not null and exists(select 1 from public.profiles where id = auth.uid() and role = 'gestor' and org_id = o.org_id))) $$;
