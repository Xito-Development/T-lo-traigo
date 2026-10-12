create or replace function public.my_role() returns public.user_role
language sql stable security definer set search_path = public as
$$ select role from public.profiles where id = auth.uid() $$;

create or replace function public.can_act_for(p_senior uuid) returns boolean
language sql stable security definer set search_path = public as
$$ select p_senior = auth.uid()
   or exists(select 1 from public.family_links where familiar_id = auth.uid() and senior_id = p_senior) $$;

create or replace function public.handle_new_user() returns trigger
language plpgsql security definer set search_path = public as $$
declare r public.user_role := 'cliente';
begin
  if new.raw_user_meta_data->>'role' = 'repartidor' then r := 'repartidor'; end if;
  insert into public.profiles(id, role, full_name, phone)
  values (new.id, r, coalesce(new.raw_user_meta_data->>'full_name',''), new.raw_user_meta_data->>'phone');
  if r = 'repartidor' then insert into public.driver_settings(driver_id) values (new.id); end if;
  return new;
end $$;
create trigger on_auth_user_created after insert on auth.users for each row execute function public.handle_new_user();

create or replace function public.protect_role() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  if new.role <> old.role and coalesce(public.my_role()::text,'') <> 'admin' then raise exception 'No autorizado a cambiar el rol'; end if;
  return new;
end $$;
create trigger profiles_protect_role before update on public.profiles for each row execute function public.protect_role();

create or replace function public.create_order(
  p_customer uuid, p_supermarket bigint, p_items jsonb,
  p_payment public.payment_method, p_address text, p_notes text default null)
returns bigint language plpgsql security definer set search_path = public as $$
declare v_town bigint; v_sub numeric := 0; v_fee numeric; v_del numeric; v_id bigint;
        cfg public.fee_config; it jsonb; pr public.products;
begin
  if auth.uid() is null or not public.can_act_for(p_customer) then raise exception 'No autorizado'; end if;
  select town_id into v_town from public.profiles where id = p_customer;
  if v_town is null then raise exception 'El cliente no tiene pueblo asignado'; end if;
  if jsonb_array_length(p_items) = 0 then raise exception 'Pedido vacío'; end if;
  select * into cfg from public.fee_config where id = 1;
  select delivery_fee into v_del from public.towns where id = v_town;
  insert into public.orders(customer_id, placed_by, town_id, supermarket_id, delivery_address,
    subtotal, service_fee, delivery_fee, total, payment_method, notes)
  values (p_customer, auth.uid(), v_town, p_supermarket, p_address, 0, 0, v_del, 0, p_payment, p_notes)
  returning id into v_id;
  for it in select * from jsonb_array_elements(p_items) loop
    select * into pr from public.products where id = (it->>'product_id')::bigint and active and supermarket_id = p_supermarket;
    if pr.id is null then raise exception 'Producto no disponible: %', it->>'product_id'; end if;
    insert into public.order_items(order_id, product_id, name, unit_price, qty)
    values (v_id, pr.id, pr.name, pr.price, (it->>'qty')::int);
    v_sub := v_sub + pr.price * (it->>'qty')::int;
  end loop;
  v_fee := greatest(round(v_sub * cfg.service_fee_pct / 100, 2), cfg.min_service_fee);
  update public.orders set subtotal = v_sub, service_fee = v_fee, total = v_sub + v_fee + v_del where id = v_id;
  return v_id;
end $$;

create or replace function public.accept_order(p_order bigint) returns void
language plpgsql security definer set search_path = public as $$
begin
  if public.my_role() <> 'repartidor' then raise exception 'Solo repartidores'; end if;
  update public.orders set driver_id = auth.uid(), status = 'aceptado', updated_at = now()
  where id = p_order and status = 'pendiente' and driver_id is null;
  if not found then raise exception 'Pedido ya asignado o inexistente'; end if;
end $$;

create or replace function public.set_order_status(p_order bigint, p_status public.order_status) returns void
language plpgsql security definer set search_path = public as $$
declare o public.orders;
begin
  select * into o from public.orders where id = p_order for update;
  if o.id is null then raise exception 'No existe'; end if;
  if p_status = 'cancelado' then
    if not (public.can_act_for(o.customer_id) and o.status = 'pendiente') and public.my_role() <> 'admin' then
      raise exception 'Solo se puede cancelar un pedido pendiente'; end if;
  elsif o.driver_id is distinct from auth.uid() and public.my_role() <> 'admin' then
    raise exception 'No autorizado';
  end if;
  update public.orders set status = p_status, updated_at = now(),
    payment_status = case when p_status = 'entregado' and payment_method = 'efectivo' then 'pagado' else payment_status end
  where id = p_order;
end $$;

create or replace function public.payment_methods_for_town(p_town bigint) returns text[]
language sql stable security definer set search_path = public as $$
  select array_remove(array[
    case when bool_or(accepts_cash) then 'efectivo' end,
    case when bool_or(accepts_bizum) then 'bizum' end,
    case when bool_or(accepts_card) then 'tarjeta' end], null)
  from public.driver_settings where available and p_town = any(town_ids) $$;

create or replace function public.link_senior(p_code text) returns uuid
language plpgsql security definer set search_path = public as $$
declare v uuid;
begin
  select id into v from public.profiles where link_code = upper(trim(p_code));
  if v is null then raise exception 'Código no válido'; end if;
  if v = auth.uid() then raise exception 'No puedes vincularte contigo mismo'; end if;
  insert into public.family_links(familiar_id, senior_id) values (auth.uid(), v) on conflict do nothing;
  update public.profiles set link_code = upper(substr(md5(gen_random_uuid()::text),1,6)) where id = v;
  return v;
end $$;

revoke execute on function public.handle_new_user(), public.protect_role() from public, anon, authenticated;
revoke execute on function public.create_order(uuid,bigint,jsonb,public.payment_method,text,text),
  public.accept_order(bigint), public.set_order_status(bigint,public.order_status),
  public.payment_methods_for_town(bigint), public.my_role(), public.can_act_for(uuid), public.link_senior(text) from public, anon;
grant execute on function public.create_order(uuid,bigint,jsonb,public.payment_method,text,text),
  public.accept_order(bigint), public.set_order_status(bigint,public.order_status),
  public.payment_methods_for_town(bigint), public.my_role(), public.can_act_for(uuid), public.link_senior(text) to authenticated;
