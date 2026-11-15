-- Wexresprise Store: table, policies and a public storage bucket.
-- Paste this whole script into Supabase -> SQL Editor -> Run.

create table if not exists public.files (
  id uuid primary key default gen_random_uuid(),
  title text not null,
  description text default '',
  size_bytes bigint not null default 0,
  mime text default 'application/octet-stream',
  storage_path text not null,
  is_wex boolean not null default false,
  created_at timestamptz not null default now()
);

alter table public.files enable row level security;

drop policy if exists "wex files read" on public.files;
create policy "wex files read"
  on public.files for select
  using (true);

drop policy if exists "wex files insert" on public.files;
create policy "wex files insert"
  on public.files for insert
  with check (true);

insert into storage.buckets (id, name, public)
values ('wexstore', 'wexstore', true)
on conflict (id) do update set public = true;

drop policy if exists "wex storage read" on storage.objects;
create policy "wex storage read"
  on storage.objects for select
  using (bucket_id = 'wexstore');

drop policy if exists "wex storage insert" on storage.objects;
create policy "wex storage insert"
  on storage.objects for insert
  with check (bucket_id = 'wexstore');
