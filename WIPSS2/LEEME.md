# WIPSS2: lo que hay que instalar (06-nov-2026)

Para la instancia **WIP SSJ2 troll** (Minecraft 1.21.1, NeoForge 21.1.264).

## 1. Bajar
En GitHub, con el botón **Code → Download ZIP** de esta rama (`claude/zealous-bohr-uqnl86`), o archivo por archivo con el botón de descarga de cada uno.

## 2. Instalar (con el juego cerrado)
| De esta carpeta | A la instancia | Qué hacer |
|---|---|---|
| `mods/wipspowers-3.5.0.jar` | `mods/` | **Borra** cualquier `wipspowers-*.jar` anterior y pon este. |
| `mods/lithium-neoforge-0.15.4+mc1.21.1.jar` | `mods/` | Nuevo. Optimiza la CPU del juego (−28 % en el tiempo por tick, medido). |
| `config/lithium.properties` | `config/` | Nuevo. Evita el choque con ModernFix. |
| `config/particle_core_config.toml` | `config/` | Reemplaza el tuyo. Solo cambia el tick asíncrono de Particle Core a `false`, porque AsyncParticles ya lo hace. |

## 3. Memoria y Java
Lo explica `optimizacion/JVM-CURSEFORGE.md`: memoria de **8192 MB** y una línea de argumentos para pegar en el perfil de CurseForge.

## 4. Perfil gráfico (opcional)
`optimizacion/aplicar-perfil.ps1`: aplica Calidad, Equilibrado o Rendimiento sin tocar texturas ni shaders. Guarda una copia de lo que cambia.

## 5. Medir FPS (antes y después)
`optimizacion/PROTOCOLO-FPS.md` y `optimizacion/planilla-fps.csv`.

## Servidor dedicado
- Mismos `mods/` y `config/lithium.properties` que arriba.
- `servidor/user_jvm_args.txt` reemplaza el del servidor.
- **Sandevistan** e **Insight Chests** van solo en el cliente: en un servidor dedicado lo crashean.

## Licencias
Lithium es de CaffeineMC, con licencia LGPL-3.0: https://github.com/CaffeineMC/lithium (jar original de Modrinth, sin modificar).
wipspowers es privado: es para el grupo, no para publicar.
