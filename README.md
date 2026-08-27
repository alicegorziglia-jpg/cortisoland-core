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
