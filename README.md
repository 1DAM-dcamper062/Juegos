# Juegos DCP

Web: [https://1dam-dcamper062.github.io/Juegos/](https://1dam-dcamper062.github.io/Juegos/)

## Multijugador online

Se ha añadido juego online para **Ajedrez**, **Damas** y **Parchís español**. Las partidas se crean mediante una sala con código y se sincronizan en tiempo real entre usuarios registrados.

### Preparación obligatoria en Supabase

1. Abre el proyecto de Supabase configurado en `index.html`.
2. En **SQL Editor**, ejecuta el archivo completo `supabase.sql` (incluye las tablas anteriores y las funciones/tablas nuevas de multijugador).
3. En **Project Settings → API**, comprueba que `index.html` conserva el Project URL y la clave publicable correctos.
4. En **Authentication → Providers → Email**, habilita Email y desactiva la confirmación de correo si quieres mantener el registro directo que usa esta web.
5. En **Database → Publications**, comprueba que `multiplayer_games` está en `supabase_realtime`. El SQL intenta añadirla automáticamente.

### Cómo jugar

1. Publica todos los archivos de esta carpeta en el mismo alojamiento y mantén sus nombres/rutas.
2. Cada participante debe entrar en la web y registrarse o iniciar sesión. El modo multijugador no funciona como invitado ni en el modo local.
3. Abre Ajedrez, Damas o Parchís.
4. Un jugador pulsa **Crear sala** y comparte el código de seis caracteres.
5. Los demás escriben el código y pulsan **Unirse**. La partida comienza cuando se ocupan todas las plazas.
6. Ajedrez y Damas admiten dos jugadores. Parchís admite dos o cuatro jugadores; selecciona el número antes de crear la sala.

### Reglas y funcionamiento

- Ajedrez conserva el motor de movimientos legales, jaque, enroque, captura al paso, promoción y tablas del juego original.
- Damas conserva las reglas españolas incluidas en el juego: captura obligatoria, elección de la captura con mayor número de piezas, peones capturando hacia delante y damas de largo alcance.
- Parchís conserva el tablero y el circuito español de 68 casillas, cuatro fichas por jugador, salida con 5, bonificación de 20 al comer, 10 al entrar en meta, barreras, seguros y regla de los tres seises.
- Las partidas se sincronizan usando Supabase Realtime y funciones SQL con control de versión para evitar sobrescribir movimientos simultáneos.

### Archivos añadidos/modificados

- `index.html`: inicializa el cliente de Supabase y comparte la conexión autenticada con los juegos.
- `multiplayer.js`: interfaz de salas, suscripción en tiempo real y envío de movimientos.
- `ajedrez.html`, `damas.html`, `parchis.html`: integración del modo online.
- `supabase.sql`: tablas, permisos y funciones SQL para crear/unirse/actualizar partidas.
