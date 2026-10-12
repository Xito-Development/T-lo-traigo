-- Aprobación de repartidores, pago simulado y mejoras de rendimiento/RLS
alter table public.driver_settings add column approved boolean not null default false;

create or replace function public.protect_role() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  if auth.uid() is not null and new.role <> old.role and coalesce(public.my_role()::text,'') <> 'admin' then
    raise exception 'No autorizado a cambiar el rol'; end if;
  return new;
end $$;

create or replace function public.protect_driver_approval() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  if auth.uid() is not null and new.approved is distinct from old.approved and coalesce(public.my_role()::text,'') <> 'admin' then
    raise exception 'Solo un administrador puede aprobar repartidores'; end if;
  return new;
end $$;
create trigger driver_protect_approval before update on public.driver_settings for each row execute function public.protect_driver_approval();
revoke execute on function public.protect_driver_approval() from public, anon, authenticated;

create or replace function public.accept_order(p_order bigint) returns void
language plpgsql security definer set search_path = public as $$
begin
  if public.my_role() not in ('repartidor','admin') then raise exception 'Solo repartidores'; end if;
  if public.my_role() = 'repartidor' and not exists(select 1 from public.driver_settings where driver_id = auth.uid() and approved) then
    raise exception 'Tu cuenta de repartidor está pendiente de aprobación'; end if;
  update public.orders set driver_id = auth.uid(), status = 'aceptado', updated_at = now()
  where id = p_order and status = 'pendiente' and driver_id is null;
  if not found then raise exception 'Este pedido ya lo ha aceptado otro repartidor'; end if;
end $$;

create or replace function public.payment_methods_for_town(p_town bigint) returns text[]
language sql stable security definer set search_path = public as $$
  select array_remove(array[
    case when bool_or(accepts_cash) then 'efectivo' end,
    case when bool_or(accepts_bizum) then 'bizum' end,
    case when bool_or(accepts_card) then 'tarjeta' end], null)
  from public.driver_settings where available and approved and p_town = any(town_ids) $$;

-- MOCKUP: sustituir por webhook de Stripe/Redsys
create or replace function public.mock_mark_paid(p_order bigint) returns void
language plpgsql security definer set search_path = public as $$
begin
  update public.orders set payment_status = 'pagado', updated_at = now()
  where id = p_order and payment_method in ('bizum','tarjeta') and public.can_act_for(customer_id);
end $$;
revoke execute on function public.mock_mark_paid(bigint) from public, anon;
grant execute on function public.mock_mark_paid(bigint) to authenticated;

-- Índices de claves foráneas
create index if not exists family_links_senior_idx on public.family_links(senior_id);
create index if not exists order_items_product_idx on public.order_items(product_id);
create index if not exists orders_placed_by_idx on public.orders(placed_by);
create index if not exists orders_supermarket_idx on public.orders(supermarket_id);
create index if not exists orders_town_idx on public.orders(town_id);
create index if not exists profiles_town_idx on public.profiles(town_id);
create index if not exists supermarkets_town_idx on public.supermarkets(town_id);

-- RLS con (select auth.uid()) para evaluarse una vez por consulta
drop policy orders_read on public.orders;
create policy orders_read on public.orders for select to authenticated using (
  public.can_act_for(customer_id) or placed_by = (select auth.uid()) or driver_id = (select auth.uid())
  or public.my_role() = 'admin'
  or (status = 'pendiente' and public.my_role() = 'repartidor'
      and exists(select 1 from public.driver_settings d where d.driver_id = (select auth.uid())
                 and d.available and d.approved and orders.town_id = any(d.town_ids))));
drop policy links_read on public.family_links; drop policy links_delete on public.family_links;
create policy links_read on public.family_links for select to authenticated using (familiar_id = (select auth.uid()) or senior_id = (select auth.uid()));
create policy links_delete on public.family_links for delete to authenticated using (familiar_id = (select auth.uid()) or senior_id = (select auth.uid()));
drop policy profile_read on public.profiles; drop policy profile_update on public.profiles;
create policy profile_read on public.profiles for select to authenticated using (
  id = (select auth.uid()) or (select public.my_role()) = 'admin'
  or exists(select 1 from public.family_links f where f.familiar_id = (select auth.uid()) and f.senior_id = profiles.id)
  or exists(select 1 from public.orders o where o.customer_id = profiles.id and o.driver_id = (select auth.uid())));
create policy profile_update on public.profiles for update to authenticated using (id = (select auth.uid()) or (select public.my_role()) = 'admin');
drop policy driver_own on public.driver_settings; drop policy driver_upd on public.driver_settings;
create policy driver_own on public.driver_settings for select to authenticated using (driver_id = (select auth.uid()) or (select public.my_role()) = 'admin');
create policy driver_upd on public.driver_settings for update to authenticated using (driver_id = (select auth.uid()) or (select public.my_role()) = 'admin');

do $$
declare t text;
begin
  foreach t in array array['towns','supermarkets','categories','products'] loop
    execute format('drop policy if exists %I on public.%I', case t when 'towns' then 'admin_towns' when 'supermarkets' then 'admin_markets' when 'categories' then 'admin_cats' else 'admin_products' end, t);
    execute format('create policy admin_ins on public.%I for insert to authenticated with check ((select public.my_role()) = ''admin'')', t);
    execute format('create policy admin_upd on public.%I for update to authenticated using ((select public.my_role()) = ''admin'')', t);
    execute format('create policy admin_del on public.%I for delete to authenticated using ((select public.my_role()) = ''admin'')', t);
  end loop;
end $$;
drop policy read_products on public.products;
drop policy if exists admin_read_products on public.products;
create policy read_products_anon on public.products for select to anon using (active);
create policy read_products on public.products for select to authenticated using (active or (select public.my_role()) = 'admin');
