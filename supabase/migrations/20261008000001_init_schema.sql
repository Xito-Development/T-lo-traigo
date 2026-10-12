-- T lo traigo · esquema base (aplicado en proyecto qqqudthqgpojbipsevna)
create type public.user_role as enum ('cliente','repartidor','admin');
create type public.order_status as enum ('pendiente','aceptado','comprando','en_camino','entregado','cancelado');
create type public.payment_method as enum ('efectivo','bizum','tarjeta');
create type public.payment_status as enum ('pendiente','pagado','fallido');

create table public.towns (
  id bigint generated always as identity primary key, name text not null unique,
  province text not null default 'León', delivery_fee numeric(6,2) not null default 3.50, active boolean not null default true);
create table public.profiles (
  id uuid primary key references auth.users(id) on delete cascade, role public.user_role not null default 'cliente',
  full_name text not null default '', phone text, address text, town_id bigint references public.towns(id),
  senior_mode boolean not null default true, created_at timestamptz not null default now(),
  link_code text unique default upper(substr(md5(gen_random_uuid()::text), 1, 6)));
create table public.family_links (
  familiar_id uuid not null references public.profiles(id) on delete cascade,
  senior_id uuid not null references public.profiles(id) on delete cascade,
  primary key (familiar_id, senior_id), check (familiar_id <> senior_id));
create table public.supermarkets (
  id bigint generated always as identity primary key, name text not null, website text,
  town_id bigint references public.towns(id), active boolean not null default true);
create table public.categories (
  id bigint generated always as identity primary key, name text not null unique,
  icon text not null default 'shopping_basket', sort int not null default 0);
create table public.products (
  id bigint generated always as identity primary key,
  supermarket_id bigint not null references public.supermarkets(id) on delete cascade,
  category_id bigint references public.categories(id), name text not null, brand text,
  unit text not null default 'ud', price numeric(8,2) not null check (price >= 0), image_url text, ean text,
  active boolean not null default true,
  source text not null default 'manual' check (source in ('manual','csv','scraping')), source_url text,
  updated_at timestamptz not null default now(), unique (supermarket_id, ean));
create index products_cat_idx on public.products(category_id) where active;
create index products_name_idx on public.products using gin (to_tsvector('spanish', name));
create table public.fee_config (
  id int primary key default 1 check (id = 1),
  service_fee_pct numeric(5,2) not null default 10.00, min_service_fee numeric(6,2) not null default 2.00);
insert into public.fee_config default values;
create table public.driver_settings (
  driver_id uuid primary key references public.profiles(id) on delete cascade,
  available boolean not null default false, accepts_cash boolean not null default true,
  accepts_bizum boolean not null default false, accepts_card boolean not null default false,
  town_ids bigint[] not null default '{}');
create table public.orders (
  id bigint generated always as identity primary key,
  customer_id uuid not null references public.profiles(id), placed_by uuid not null references public.profiles(id),
  driver_id uuid references public.profiles(id), town_id bigint not null references public.towns(id),
  supermarket_id bigint not null references public.supermarkets(id), delivery_address text not null,
  status public.order_status not null default 'pendiente', subtotal numeric(10,2) not null,
  service_fee numeric(8,2) not null, delivery_fee numeric(8,2) not null, total numeric(10,2) not null,
  payment_method public.payment_method not null, payment_status public.payment_status not null default 'pendiente',
  notes text, created_at timestamptz not null default now(), updated_at timestamptz not null default now());
create index orders_customer_idx on public.orders(customer_id);
create index orders_driver_idx on public.orders(driver_id);
create index orders_status_idx on public.orders(status, town_id);
create table public.order_items (
  id bigint generated always as identity primary key,
  order_id bigint not null references public.orders(id) on delete cascade,
  product_id bigint references public.products(id) on delete set null, name text not null,
  unit_price numeric(8,2) not null, qty int not null check (qty > 0 and qty <= 99));
create index order_items_order_idx on public.order_items(order_id);
alter publication supabase_realtime add table public.orders;
