-- Herramientas de depuración (solo admin): generar, avanzar y borrar pedidos de prueba
alter table public.orders add column is_test boolean not null default false;
create index orders_test_idx on public.orders(is_test) where is_test;

create or replace function public.debug_spawn_orders(p_count int default 1, p_town bigint default null, p_payment public.payment_method default null)
returns int language plpgsql security definer set search_path = public as $$
declare i int; v_id bigint; v_town bigint; v_sub numeric; v_fee numeric; v_del numeric; v_market bigint; cfg public.fee_config;
  streets text[] := array['Calle Mayor','Calle Real','Plaza de la Iglesia','Calle del Río','Travesía La Fuente','Camino de la Ermita'];
begin
  if public.my_role() is distinct from 'admin' then raise exception 'Solo administradores'; end if;
  if p_count not between 1 and 20 then raise exception 'Entre 1 y 20 pedidos'; end if;
  select * into cfg from public.fee_config where id = 1;
  select id into v_market from public.supermarkets where active order by id limit 1;
  for i in 1..p_count loop
    v_town := coalesce(p_town, (select id from public.towns where active order by random() limit 1));
    select delivery_fee into v_del from public.towns where id = v_town;
    insert into public.orders(customer_id, placed_by, town_id, supermarket_id, delivery_address,
      subtotal, service_fee, delivery_fee, total, payment_method, notes, is_test)
    values (auth.uid(), auth.uid(), v_town, v_market,
      streets[1 + floor(random() * array_length(streets, 1))::int] || ' ' || (1 + floor(random() * 40))::int,
      0, 0, v_del, 0,
      coalesce(p_payment, (array['efectivo','bizum','tarjeta']::public.payment_method[])[1 + floor(random() * 3)::int]),
      '[PRUEBA] Pedido generado automáticamente', true)
    returning id into v_id;
    insert into public.order_items(order_id, product_id, name, unit_price, qty)
    select v_id, id, name, price, 1 + floor(random() * 3)::int
    from public.products where active and supermarket_id = v_market order by random() limit 2 + floor(random() * 5)::int;
    select coalesce(sum(unit_price * qty), 0) into v_sub from public.order_items where order_id = v_id;
    v_fee := greatest(round(v_sub * cfg.service_fee_pct / 100, 2), cfg.min_service_fee);
    update public.orders set subtotal = v_sub, service_fee = v_fee, total = v_sub + v_fee + v_del where id = v_id;
  end loop;
  return p_count;
end $$;

create or replace function public.debug_advance_test_orders() returns int
language plpgsql security definer set search_path = public as $$
declare n int;
begin
  if public.my_role() is distinct from 'admin' then raise exception 'Solo administradores'; end if;
  update public.orders set
    status = case status when 'pendiente' then 'aceptado' when 'aceptado' then 'comprando'
                         when 'comprando' then 'en_camino' else 'entregado' end::public.order_status,
    driver_id = coalesce(driver_id, auth.uid()),
    payment_status = case when status = 'en_camino' and payment_method = 'efectivo' then 'pagado' else payment_status end,
    updated_at = now()
  where is_test and status not in ('entregado','cancelado');
  get diagnostics n = row_count;
  return n;
end $$;

create or replace function public.debug_clear_test_orders() returns int
language plpgsql security definer set search_path = public as $$
declare n int;
begin
  if public.my_role() is distinct from 'admin' then raise exception 'Solo administradores'; end if;
  delete from public.orders where is_test;
  get diagnostics n = row_count;
  return n;
end $$;

revoke execute on function public.debug_spawn_orders(int,bigint,public.payment_method), public.debug_advance_test_orders(), public.debug_clear_test_orders() from public, anon;
grant execute on function public.debug_spawn_orders(int,bigint,public.payment_method), public.debug_advance_test_orders(), public.debug_clear_test_orders() to authenticated;
