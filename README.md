# Dificultad Progresiva

Mod para Minecraft 1.21.1 con NeoForge 21.1.248. Permite subir manualmente un nivel de dificultad por mundo cuando quieras: semanalmente, diariamente o en cualquier momento.

## Comandos

Requieren permisos de operador nivel 2.

```mcfunction
/dificultadprogresiva ver
/dificultadprogresiva nivel <nivel>
/dificultadprogresiva configurar vida_por_nivel <porcentaje>
/dificultadprogresiva configurar danio_por_nivel <porcentaje>
```

## Peligros toggleables

Requieren permisos de operador nivel 2. Cada comando activa/desactiva el peligro (empiezan todos desactivados y se guardan por mundo).

```mcfunction
/lava             - La lava purificadora quita la resistencia al fuego.
/dmgdoors         - Las puertas hacen danio al abrirlas.
/dmgbuttons       - Los botones hacen danio al usarlos.
/phantomshuffle   - Los phantoms ciegan y mezclan la hotbar al atacar.
/plaguecontrol    - Evita el spawn de animales salvo domesticados, bebes, nombrados o en botes.
/radiation        - Los cultivos (trigo, zanahorias, cacao, nether wart, tallos, etc.) dejan de crecer.
/fiebretejedora   - Las aranias tejen telarania en tus pies al atacarte.
/fiebretaracnida  - Destruir telaranias invoca una arania de cueva.
/fuegoeterno      - El fuego nunca se apaga, salvo bajo el agua.
/nobreathing      - Las puertas ya no dan una bolsa de aire para respirar bajo el agua.
/infernus         - El danio de fuego se duplica.
/lavamortal       - La lava mata al instante.
/piesdebiles      - El danio de caida aumenta muchisimo.
/totemmesagges    - Activa/desactiva el aviso en el chat cada vez que se consume un totem.
/totemdebil       - Los totems normales solo funcionan el 50% de las veces.
```

## Portado del plugin dedsafio (events/changes)

```mcfunction
/doorsinstakill       - Abrir una puerta mata al instante (solo en survival, los ops no mueren).
/buttonsinstakill     - Usar un boton mata al instante (solo en survival, los ops no mueren).
/disablenether        - Cancela el viaje al Nether por portal.
/electriccreepers     - Los creepers siempre spawnean cargados (electricos).
/novillagerbreeding   - Evita que nazcan aldeanos bebe por reproduccion.
/enderpearlhalfhealth - Usar una enderperla te quita la mitad de tu vida actual (en survival).
/piglinsnuggets       - Los piglins sueltan 3-5 pepitas de oro al morir.
/golemswardens        - Los golems de hierro se reemplazan por wardens al spawnear.
```

Nota: `SpiderWebsOnHit` y `BreakWebsSpawnPoisonousSpiders` del plugin no se portaron por separado porque son identicas a `/fiebretejedora` y `/fiebretaracnida`, que ya existian en este mod.

## Items custom (portados del plugin dedsafio)

Todavia no estan en una pestaña creativa ni en un comando `/items` (eso viene con el sistema de menus, en otra sesion). Por ahora se consiguen con `/give`:

```mcfunction
/give @s progressivedifficulty:sunblock
/give @s progressivedifficulty:ghost_sword
/give @s progressivedifficulty:blue_capsule
/give @s progressivedifficulty:fork                    - clic derecho: mensaje "aun no disponible" (igual que el plugin).
/give @s progressivedifficulty:spoon                    - idem fork.
/give @s progressivedifficulty:infernal_sword           - cada golpe conectado cura +1 corazon a quien la usa.
/give @s progressivedifficulty:ender_bag                - clic derecho: abre tu ender chest desde cualquier lado.
/give @s progressivedifficulty:spawn_stick              - golpear a un jugador lo teletransporta a su punto de spawn (sin danio).
/give @s progressivedifficulty:portable_golden_anvil    - clic derecho: repara toda tu armadura puesta y se consume 1 unidad.
```

## Sistema de muerte / eliminacion

```mcfunction
/sistemamuerte
```
Toggle (empieza desactivado). Con esto activado, cuando un jugador (no-op) muere: se lo pasa a modo espectador, queda marcado como "muerto" (persistido), se reproduce la animacion real de "muerte" (los 92 frames de tu resourcepack) a todos los jugadores con su sonido, y a los `/sistemamuerte`-segundos configurados (10 por defecto) se lo expulsa del servidor. Si intenta reconectarse mientras sigue muerto, se lo vuelve a expulsar automaticamente.

```mcfunction
/revive <jugador> <avisar:true|false>
```
Revive a un jugador (acepta jugadores desconectados via el selector de perfil). Si `avisar` es `false`, se le muestra un anuncio de "ha resucitado" la proxima vez que entre y suma a su contador de veces revivido.

## Fogata (zona de resurreccion)

```mcfunction
/fogata set      - te da la herramienta de marcacion (marker_item)
/fogata apply    - guarda el perimetro marcado como zona de fogata
/fogata remove   - borra la zona de fogata
/fogata help
```

```mcfunction
/give @s progressivedifficulty:resurrection_spoon
```
Clic derecho dentro de la zona de fogata (con alma) muestra en el chat una lista clickeable de jugadores muertos - click en un nombre para revivirlo, gasta 1 alma propia y 1 cuchara.

**Simplificacion importante:** el menu original del plugin (`FogataMenu`) es un GUI paginado con cabezas de jugador 3D. Lo cambie por una lista de texto clickeable en el chat -el resultado funcional es el mismo (elegis a quien revivir con un click)- para evitar construir un `AbstractContainerMenu`/`Screen` custom desde cero (mucho mas riesgo de errores de compilacion sin poder probar localmente). Si despues querés el GUI real con cabezas, es una sesion aparte.

## Join / Quit

Al entrar: si el jugador esta marcado como muerto, se lo expulsa de nuevo; si tiene un aviso de resurreccion pendiente, se anuncia a todo el server; si estaba en espectador (por el sistema de muerte) queda en survival.

**Limitacion real:** no pude suprimir el mensaje vanilla de "X se unio a la partida/X salio de la partida" sin meter un Mixin en `PlayerList` (mas riesgo de compilacion). Por ahora conviven: el mensaje vanilla de siempre, mas el anuncio de resurreccion cuando corresponde.

## /dedsafio

```mcfunction
/dedsafio
/dedsafio version
```
Ayuda basica y version del mod (no hay un "reload" real ya que este mod no usa un sistema de config recargable en caliente).

## Herramienta de marcacion

```mcfunction
/give @s progressivedifficulty:marker_item
```

Golden-hoe-style, port de `MarkerItem`: **clic izquierdo** en un bloque define la posicion 1, **clic derecho** define la posicion 2. Las posiciones quedan guardadas en el propio item (NBT), no se puede soltar (`Q`) mientras las tenga. Todavia no esta conectado a nada (el plugin la usaba para marcar la fogata de resurreccion) - queda lista para cuando portemos ese sistema.

## /soul

```mcfunction
/soul <jugador>
/soul set <jugador> <true|false>
```

Bandera booleana por jugador ("tiene alma" o no), persistida por mundo. Por defecto todos tienen alma (`true`), igual que el plugin original. Es la unica pieza portada del sistema de usuarios (`User`/`UserManager`) - el resto (`dead`, `revived-times`, `alert-revive`) pertenece al sistema de revivir, que se dejo afuera.

Las texturas de estos 8 items son placeholders generados a mano (pixel art simple 16x16), no el arte original del plugin. Reemplazalas cuando quieras en `src/main/resources/assets/progressivedifficulty/textures/item/`.

## Ruleta (portado del plugin dedsafio)

```mcfunction
/ruleta <tipo> <color> <jugadores> <mensaje...>
```

- `tipo`: `title`, `subtitle`, `actionbar` o `sidebar` (sidebar cae a actionbar, no se implemento un scoreboard real).
- `color`: `red`, `orange`, `yellow`, `green`, `blue`, `purple`, `pink`, `cyan`.
- `jugadores`: selector de entidades, ej. `@a`, `NombreJugador`.
- `mensaje`: el texto final que se revela (todo lo que sigue, greedy).

Ejemplo: `/ruleta actionbar orange @a Sube el nivel de dificultad a 3`

**Importante:** este comando ahora usa los frames REALES de tu resourcepack (`dedsafio-textures-1_0_0-alpha`), copiados dentro del propio mod bajo una fuente propia `progressivedifficulty:ruleta` (así los jugadores no necesitan instalar ningun resourcepack aparte, viene incluido en el mod). Se porto:
- Los 8 giros de color (blue/cyan/green/orange/pink/purple/red/yellow), fotograma por fotograma, al mismo ritmo original (~1 frame por tick).
- Los iconos `{r-color}` de tu `config.yml` (fuente `progressivedifficulty:icons`).
- Los sonidos `ruleta.ogg` y `muerte.ogg`, empaquetados como `progressivedifficulty:ruleta` / `progressivedifficulty:muerte`.
- Las texturas reales de los items (`sunblock`, `fork`, `spoon`, `blue_capsule`, `infernal_sword`, `ender_bag`, `portable_golden_anvil`, y `resurrection_spoon` para mas adelante).

**No incluido en el pack alpha que me pasaste** (asi que sigue con fallback de texto plano): las animaciones "Reviil" y "Nutria" (la segunda etapa de los colores rojo/rosa) y la textura real de `ghost_sword`. Si en algun momento tenes esos frames, los sumamos igual.

## Comandos sueltos portados esta sesion

```mcfunction
/eon <mensaje>
```
Anuncio formateado a todo el server (equivalente a `/eon` del plugin).

```mcfunction
/config set <clave> <valor>
/config get <clave>
```
Guarda/lee valores de texto libres, persistidos por mundo (para lo que necesites mas adelante, no tiene claves fijas).

```mcfunction
/dtp <jugadores> <x> <y> <z> <fadeIn> <stay> <fadeOut>
```
Flash de pantalla blanca (titulo) y luego teletransporta a los jugadores a esa posicion tras `stay + fadeOut` ticks. Solo dentro de la misma dimension del que ejecuta el comando (no cruza dimensiones).

```mcfunction
/timer add <segundos> <color> <estilo> <nombre>
/timer remove <nombre>
```
Boss bar de cuenta regresiva visible para todos los jugadores conectados. `color`: red/pink/blue/green/yellow/purple/white. `estilo`: solid (por defecto), 6/10/12/20 (segmentado).

```mcfunction
/horario activar
/horario desactivar
/horario abrir <HH:MM>
/horario cerrar <HH:MM>
```
Puerto de TimeController: activa un horario de apertura/cierre del server. En la hora de cierre, expulsa a los jugadores sin op y activa la whitelist, con una boss bar de cuenta regresiva en los ultimos 5 minutos. Se revisa una vez por minuto de juego (1200 ticks).

```mcfunction
/items
```
Abre un menu tipo cofre con los items custom del mod - tomá el que quieras, se genera una copia fresca cada vez que se ejecuta el comando (no "se acaba" el catalogo).

## Totems verdaderos

```mcfunction
/totemverdadero [cantidad]
```

Entrega al jugador que ejecuta el comando totems especiales que **siempre** funcionan al 100%, incluso con `/totemdebil` activado. Se llaman "Totem Nutria" y tienen su propia textura (un otter/nutria) via Custom Model Data sobre el totem de la inmortalidad vanilla - el modelo override vive en `assets/minecraft/models/item/totem_of_undying.json` dentro del mod (es la unica excepcion a usar solo el namespace propio: hace falta para reskinnear un item vanilla especifico).

## Valores por defecto

- Nivel `0`: vanilla.
- Vida por nivel: `20%`.
- Danio por nivel: `10%`.

Ejemplo: con nivel `3`, los enemigos tienen `+60%` vida y hacen `+30%` danio.

## Persistencia

La configuracion se guarda en cada mundo como:

```text
progressive_difficulty.properties
```

## Compilar

```powershell
.\gradlew.bat build
```

El mod compilado queda en:

```text
build/libs/progressivedifficulty-1.0.0.jar
```
