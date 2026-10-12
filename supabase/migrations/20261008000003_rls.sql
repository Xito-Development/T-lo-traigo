alter table public.towns enable row level security;
alter table public.profiles enable row level security;
alter table public.family_links enable row level security;
alter table public.supermarkets enable row level security;
alter table public.categories enable row level security;
alter table public.products enable row level security;
alter table public.fee_config enable row level security;
alter table public.driver_settings enable row level security;
alter table public.orders enable row level security;
alter table public.order_items enable row level security;

-- Catálogo público
create policy read_towns on public.towns for select using (true);
create policy read_markets on public.supermarkets for select using (true);
create policy read_cats on public.categories for select using (true);
create policy read_products on public.products for select using (active);
create policy read_fees on public.fee_config for select using (true);
-- Admin
create policy admin_read_products on public.products for select to authenticated using (public.my_role() = 'admin');
create policy admin_towns on public.towns for all to authenticated using (public.my_role()='admin') with check (public.my_role()='admin');
create policy admin_markets on public.supermarkets for all to authenticated using (public.my_role()='admin') with check (public.my_role()='admin');
create policy admin_cats on public.categories for all to authenticated using (public.my_role()='admin') with check (public.my_role()='admin');
create policy admin_products on public.products for all to authenticated using (public.my_role()='admin') with check (public.my_role()='admin');
create policy admin_fees on public.fee_config for update to authenticated using (public.my_role()='admin');
-- Perfiles / familia
create policy profile_read on public.profiles for select to authenticated using (
  id = auth.uid() or public.my_role() = 'admin'
  or exists(select 1 from public.family_links f where f.familiar_id = auth.uid() and f.senior_id = profiles.id)
  or exists(select 1 from public.orders o where o.customer_id = profiles.id and o.driver_id = auth.uid()));
create policy profile_update on public.profiles for update to authenticated using (id = auth.uid() or public.my_role()='admin');
create policy links_read on public.family_links for select to authenticated using (familiar_id = auth.uid() or senior_id = auth.uid());
create policy links_delete on public.family_links for delete to authenticated using (familiar_id = auth.uid() or senior_id = auth.uid());
-- Repartidores
create policy driver_own on public.driver_settings for select to authenticated using (driver_id = auth.uid() or public.my_role()='admin');
create policy driver_upd on public.driver_settings for update to authenticated using (driver_id = auth.uid());
-- Pedidos (escritura solo vía RPC)
create policy orders_read on public.orders for select to authenticated using (
  public.can_act_for(customer_id) or placed_by = auth.uid() or driver_id = auth.uid() or public.my_role() = 'admin'
  or (status = 'pendiente' and public.my_role() = 'repartidor'
      and exists(select 1 from public.driver_settings d where d.driver_id = auth.uid() and d.available and orders.town_id = any(d.town_ids))));
create policy items_read on public.order_items for select to authenticated using (
  exists(select 1 from public.orders o where o.id = order_items.order_id));
