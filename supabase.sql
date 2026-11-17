-- Ejecuta esto en Supabase > SQL Editor.
-- Antes: Authentication > Providers > Email > desactiva "Confirm email".

create table progress (
  user_id uuid primary key references auth.users on delete cascade,
  data jsonb not null default '{}',
  updated_at timestamptz default now()
);
create table scores (
  id bigint generated always as identity primary key,
  game text not null,
  user_id uuid not null references auth.users on delete cascade,
  username text not null,
  score int not null check (score >= 0),
  created_at timestamptz default now()
);
create index on scores (game, score desc);
create table bugs (
  id bigint generated always as identity primary key,
  game text not null,
  text text not null check (char_length(text) between 10 and 2000),
  username text,
  created_at timestamptz default now()
);

alter table progress enable row level security;
alter table scores enable row level security;
alter table bugs enable row level security;

create policy "progreso propio" on progress for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "ver clasificación" on scores for select using (true);
create policy "subir mi puntuación" on scores for insert with check (auth.uid() = user_id);
create policy "enviar bugs" on bugs for insert with check (true);
-- Los bugs solo los ves tú desde el panel de Supabase (Table Editor > bugs).
