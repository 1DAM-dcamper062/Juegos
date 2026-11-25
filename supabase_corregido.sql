-- Ejecuta esto en Supabase > SQL Editor.
-- Antes: Authentication > Providers > Email > desactiva "Confirm email".

create table if not exists public.progress (
  user_id uuid primary key references auth.users on delete cascade,
  data jsonb not null default '{}',
  updated_at timestamptz default now()
);
create table if not exists public.scores (
  id bigint generated always as identity primary key,
  game text not null,
  user_id uuid not null references auth.users on delete cascade,
  username text not null,
  score int not null check (score >= 0),
  created_at timestamptz default now()
);
create index if not exists scores_game_score_idx on public.scores (game, score desc);
create table if not exists public.bugs (
  id bigint generated always as identity primary key,
  game text not null,
  text text not null check (char_length(text) between 10 and 2000),
  username text,
  created_at timestamptz default now()
);

alter table public.progress enable row level security;
alter table public.scores enable row level security;
alter table public.bugs enable row level security;

drop policy if exists "progreso propio" on public.progress;
create policy "progreso propio" on public.progress
  for all to authenticated using (auth.uid() = user_id) with check (auth.uid() = user_id);

drop policy if exists "ver clasificación" on public.scores;
create policy "ver clasificación" on public.scores
  for select to anon, authenticated using (true);

drop policy if exists "subir mi puntuación" on public.scores;
create policy "subir mi puntuación" on public.scores
  for insert to authenticated with check (auth.uid() = user_id);

drop policy if exists "enviar bugs" on public.bugs;
create policy "enviar bugs" on public.bugs
  for insert to anon, authenticated with check (true);
-- Los bugs solo los ves tú desde el panel de Supabase (Table Editor > bugs).


-- ===== Partidas multijugador en tiempo real =====
create table if not exists public.multiplayer_games (
  id uuid primary key default gen_random_uuid(),
  code text not null unique,
  game text not null check (game in ('ajedrez','damas','parchis')),
  host_id uuid not null references auth.users(id) on delete cascade,
  player_ids uuid[] not null,
  capacity int not null check (capacity in (2,4)),
  state jsonb not null,
  turn_seat int not null default 0,
  status text not null default 'waiting' check (status in ('waiting','playing','finished')),
  version bigint not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  check (cardinality(player_ids) <= capacity),
  check (turn_seat >= 0 and turn_seat < capacity)
);
create index if not exists multiplayer_games_code_idx on public.multiplayer_games(code);
alter table public.multiplayer_games enable row level security;
drop policy if exists "jugadores pueden ver su partida" on public.multiplayer_games;
create policy "jugadores pueden ver su partida" on public.multiplayer_games
  for select to authenticated using (auth.uid() = any(player_ids));

create or replace function public.mp_create_game(p_game text,p_capacity int,p_state jsonb)
returns public.multiplayer_games
language plpgsql security definer set search_path=public
as $$
declare v public.multiplayer_games; v_code text;
begin
  if auth.uid() is null then raise exception 'Inicia sesión para crear una sala'; end if;
  if p_game not in ('ajedrez','damas','parchis') then raise exception 'Juego no válido'; end if;
  if p_capacity not in (2,4) or (p_game <> 'parchis' and p_capacity <> 2) then raise exception 'Número de jugadores no válido'; end if;
  loop
    v_code := upper(substr(encode(gen_random_bytes(5),'hex'),1,6));
    exit when not exists(select 1 from public.multiplayer_games where code=v_code and status <> 'finished');
  end loop;
  insert into public.multiplayer_games(code,game,host_id,player_ids,capacity,state,turn_seat,status)
  values(v_code,p_game,auth.uid(),array[auth.uid()],p_capacity,p_state,0,'waiting') returning * into v;
  return v;
end $$;

create or replace function public.mp_join_game(p_code text)
returns public.multiplayer_games
language plpgsql security definer set search_path=public
as $$
declare v public.multiplayer_games;
begin
  if auth.uid() is null then raise exception 'Inicia sesión para unirte'; end if;
  select * into v from public.multiplayer_games where code=upper(trim(p_code)) for update;
  if not found then raise exception 'No existe esa sala'; end if;
  if v.status='finished' then raise exception 'La partida ya ha terminado'; end if;
  if auth.uid()=any(v.player_ids) then return v; end if;
  if cardinality(v.player_ids)>=v.capacity then raise exception 'La sala está completa'; end if;
  update public.multiplayer_games
    set player_ids=array_append(player_ids,auth.uid()),
        status=case when cardinality(player_ids)+1=capacity then 'playing' else 'waiting' end,
        updated_at=now(),version=version+1
    where id=v.id returning * into v;
  return v;
end $$;

create or replace function public.mp_submit_move(
  p_game_id uuid,p_expected_version bigint,p_state jsonb,p_turn_seat int,p_status text default 'playing'
) returns public.multiplayer_games
language plpgsql security definer set search_path=public
as $$
declare v public.multiplayer_games;
begin
  if auth.uid() is null then raise exception 'Sesión no válida'; end if;
  select * into v from public.multiplayer_games where id=p_game_id for update;
  if not found then raise exception 'Partida no encontrada'; end if;
  if cardinality(v.player_ids) < v.capacity then raise exception 'Esperando a que se unan todos los jugadores'; end if;
  if v.status='finished' then raise exception 'La partida ha terminado'; end if;
  if v.version <> p_expected_version then raise exception 'La partida ha cambiado. Se ha actualizado el tablero'; end if;
  if v.player_ids[v.turn_seat+1] <> auth.uid() then raise exception 'No es tu turno'; end if;
  if p_turn_seat < 0 or p_turn_seat >= v.capacity then raise exception 'Turno no válido'; end if;
  if p_status not in ('playing','finished') then raise exception 'Estado no válido'; end if;
  update public.multiplayer_games set state=p_state,turn_seat=p_turn_seat,status=p_status,
    updated_at=now(),version=version+1 where id=p_game_id returning * into v;
  return v;
end $$;

revoke all on function public.mp_create_game(text,int,jsonb) from public;
revoke all on function public.mp_join_game(text) from public;
revoke all on function public.mp_submit_move(uuid,bigint,jsonb,int,text) from public;
grant execute on function public.mp_create_game(text,int,jsonb) to authenticated;
grant execute on function public.mp_join_game(text) to authenticated;
grant execute on function public.mp_submit_move(uuid,bigint,jsonb,int,text) to authenticated;
grant select on public.multiplayer_games to authenticated;

-- Habilita actualizaciones en Supabase Realtime.
do $$ begin
  alter publication supabase_realtime add table public.multiplayer_games;
exception when duplicate_object then null;
when undefined_object then null;
end $$;
