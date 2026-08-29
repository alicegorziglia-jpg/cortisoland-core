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

Quedan pendientes para una proxima sesion: `marker_item` (herramienta de marcacion de posiciones) y `resurrection_spoon` (dependen del sistema de fogata/resurreccion y base de datos de usuarios, que todavia no se portaron).

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

## Totems verdaderos

```mcfunction
/totemverdadero [cantidad]
```

Entrega al jugador que ejecuta el comando totems especiales que **siempre** funcionan al 100%, incluso con `/totemdebil` activado. Se identifican por su nombre "Totem Verdadero".

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
