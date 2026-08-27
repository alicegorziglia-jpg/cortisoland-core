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
