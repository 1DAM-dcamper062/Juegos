# Zombie Robot Arena

## Descripción del juego

**Zombie Robot Arena** es un juego de acción 2D desarrollado en Java. El jugador controla un personaje dentro de una arena y debe enfrentarse a oleadas de zombies y robots.

El objetivo es sobrevivir a las diferentes oleadas, conseguir puntos y monedas al eliminar enemigos, mejorar las armas y avanzar por los niveles. Durante la partida también aparecen jefes con mucha más vida y ataques especiales.

### Características principales

- 50 niveles.
- Diferentes niveles de dificultad.
- Varias oleadas de enemigos por nivel.
- Jefes finales con mucha vida.
- Diferentes armas.
- Mejoras de armas.
- Diferentes skins para el personaje.
- Monedas que dejan los enemigos al morir.
- Tienda/arsenal para comprar mejoras.
- Sistema de vida, puntos y monedas.
- Efectos visuales y partículas.
- Movimiento con teclado y apuntado con el ratón.
- Sistema de pausa.
- Pantalla de Game Over.
- Sistema de progresión por niveles y oleadas.

---

## Instalación en Windows

### 1. Instalar Java

Para ejecutar el juego necesitas tener instalado el **JDK de Java** en el ordenador.

Se recomienda utilizar **Java 17, Java 21 o una versión posterior compatible**.

Después de instalarlo, abre el **Símbolo del sistema (CMD)** y escribe:

```text
java -version
```

También puedes comprobar que el compilador está instalado con:

```text
javac -version
```

Si ambos comandos muestran una versión de Java, la instalación es correcta.

---

### 2. Descargar el juego

Descarga el archivo:

```text
ZombieRobotArena.java
```

Crea una carpeta para el juego, por ejemplo:

```text
C:\ZombieRobotArena
```

y coloca dentro el archivo `ZombieRobotArena.java`.

La carpeta debería quedar así:

```text
C:\ZombieRobotArena
    └── ZombieRobotArena.java
```

---

### 3. Compilar el juego

Abre la carpeta donde está el archivo.

Puedes hacerlo desde el Explorador de archivos escribiendo `cmd` en la barra de direcciones.

En la ventana de CMD ejecuta:

```text
javac ZombieRobotArena.java
```

Si no aparece ningún error, el juego se ha compilado correctamente.

Esto generará el archivo:

```text
ZombieRobotArena.class
```

---

### 4. Ejecutar el juego

En la misma ventana de CMD escribe:

```text
java ZombieRobotArena
```

Se abrirá la ventana del juego.

---

## Cómo jugar

### Controles

| Acción | Control |
|---|---|
| Moverse | `W`, `A`, `S`, `D` |
| Apuntar | Ratón |
| Disparar | Botón izquierdo del ratón |
| Pausar | `P` |
| Abrir arsenal/tienda | `B` |
| Reiniciar después de morir | `R` |

### Objetivo

El objetivo principal es **sobrevivir y eliminar a todos los enemigos** de cada oleada.

Al eliminar enemigos puedes conseguir:

- Puntos.
- Monedas.
- Progreso hacia la siguiente oleada.

Las monedas pueden utilizarse para mejorar el equipamiento del personaje.

### Niveles y jefes

El juego cuenta con **50 niveles**. Cada nivel está formado por varias oleadas de enemigos.

Algunas oleadas terminan con la aparición de un **jefe**, que tiene mucha más vida y es más peligroso que los enemigos normales.

Para avanzar tendrás que completar las oleadas y derrotar a los jefes.

### Armas y mejoras

Durante la partida puedes cambiar de arma y mejorar su rendimiento.

Las mejoras permiten aumentar las características de las armas para enfrentarte a enemigos cada vez más fuertes.

También puedes desbloquear diferentes **skins** para cambiar el aspecto del personaje.

### Pausa

Puedes pulsar `P` durante la partida para pausar el juego.

---

## Solución de problemas

### 'javac' no se reconoce como un comando

Significa que Java no está instalado correctamente o que la carpeta `bin` del JDK no está añadida al `PATH` de Windows.

Comprueba la instalación ejecutando:

```text
java -version
```

y:

```text
javac -version
```

### Error al ejecutar `java ZombieRobotArena`

Comprueba que estás dentro de la carpeta donde se encuentra el archivo `.class`.

Puedes comprobar la carpeta actual con:

```text
dir
```

Deberías ver:

```text
ZombieRobotArena.java
ZombieRobotArena.class
```

Después ejecuta:

```text
java ZombieRobotArena
```

---

## Tecnologías utilizadas

- **Java**
- **Java Swing**
- **Java AWT**
- Programación orientada a objetos.
- Eventos de teclado y ratón.
- Temporizadores.
- Gráficos 2D.
- Sistema de colisiones.
- Enemigos y oleadas.
- Sistema de puntos y monedas.
- Sistema de armas y mejoras.

---

## Estructura del proyecto

```text
ZombieRobotArena/
│
├── ZombieRobotArena.java
└── README.md
```

El juego está desarrollado en un único archivo Java para facilitar su instalación y ejecución.

---

## Ejecución rápida

Una vez instalado Java y descargado el archivo, abre CMD en la carpeta del juego y ejecuta:

```text
javac ZombieRobotArena.java
java ZombieRobotArena
```

Con esos dos comandos puedes compilar y ejecutar el juego desde cero en un PC con Windows.
