<div align="center">

<img alt="Factions Mod Icon" src="src/main/resources/assets/factions/icon.png">

# Factions Mod

Highly customizable, lightweight and elegant factions mod for the [Fabric loader][fabric] in Minecraft 1.19

[![Release](https://img.shields.io/github/v/release/ickerio/factions?style=for-the-badge&include_prereleases&sort=semver)][github:releases]
[![Available For](https://img.shields.io/badge/dynamic/json?label=Available%20For&style=for-the-badge&color=e64626&query=version&url=https%3A%2F%2Fapi.blueish.dev%2Fapi%2Fminecraft%2Fversion%3Fid%3Dfactions)][modrinth]
 [![Downloads](https://img.shields.io/badge/dynamic/json?label=Downloads&style=for-the-badge&color=e64626&query=downloads&url=https%3A%2F%2Fapi.blueish.dev%2Fapi%2Fminecraft%2Fdownloads%3Fcurseid%3D497362%26modrinthid%3Dfactions)][modrinth:releases]

</div>

### **ABOUT**

Factions Mod is an ultra lightweight, fast, and elegant solution to factions in modern minecraft. The **server-side** mod expands upon all the classic factions features whilst also focusing on customization and performance. Grow your faction, expand your claims, and storm your enemies for their chunks and loot.

A faction's power cap increases as new members join, expanding their ability to claim more land. For each claim they make, it requires that faction to sustain more power. Dying to other players will temporarily lose faction power and if it drops below the required threshold, all their claims will be vulnerable to being overtaken.

&nbsp;

### **FEATURES**

- 🎯 Fully featured factions mod with over 30 [commands][wiki:commands]
- ✨ Faction ranks, colors, MOTD and descriptions
- 🎉 In faction private chat, global chat and a stylized player list
- ⚡ Extreme performance and reliability
- ⚙️ Advanced [configuration][wiki:config] and customization options
- 🔥 Dynmap and Lucko Perms support out the box
- 🚀 Event driven API for further extensibility 
- 💬 Strong [community][discord] and active developer support

&nbsp;

### **GET STARTED**

Factions Mod is very intuitive and works immediately after installation, requiring no additional configuration. However, you can read further about the mod on the [Wiki][wiki]. Our wiki goes in depth about the factions mechanics, its configuration, commands and integrations.

A list of all **commands** is available on our [wiki][wiki:commands]

Have an issue or a suggestion? Join [our discord][discord]

### **License**
[MIT](LICENSE)

[fabric]: https://fabricmc.net/
[modrinth]: https://modrinth.com/mod/factions
[modrinth:releases]: https://modrinth.com/mod/factions/versions
[github:releases]: https://github.com/ickerio/factions/releases
[wiki]: https://github.com/ickerio/factions/wiki
[wiki:config]: https://github.com/ickerio/factions/wiki/Config
[wiki:commands]: https://github.com/ickerio/factions/wiki/Commands
[discord]: https://discord.gg/tHPFegeAY8

---

## Port a NeoForge 26.1.2 — notas de build

- **Requisito**: un JDK 25 *completo* (con `javac` y `lib/ct.sym`); una instalación recortada
  sin `ct.sym` falla con `release version 25 not supported` al recompilar los artefactos de
  Minecraft (ModDevGradle usa `--release 25`). El JDK del sistema en
  `/usr/java/jdk-25-oracle-aarch64` ya es completo:

  ```bash
  JAVA_HOME=/usr/java/jdk-25-oracle-aarch64 ./gradlew build
  ```

  (Durante parte del desarrollo se usó el Temurin 25 auto-provisionado por Gradle en
  `~/.gradle/jdks/`, válido como alternativa.)
- Nota de entorno: el paquete `git` del sistema desapareció transitoriamente durante una
  sesión (posiblemente tras un `dnf update`); fue restaurado manualmente. El historial del
  repo nunca se vio afectado.

- **Test en runtime** (`./gradlew runServer`): el server llega a `Done` con el mod y los 10 mixins
  aplicados, sin errores, y genera `config/factions.json` + `factions/*.dat`.
- En esta máquina hay otro proceso escuchando en `0.0.0.0:25565`; usar `server-port=25599`
  (y `server-ip=127.0.0.1`, sin IPv6 usable) en `run/server.properties` para pruebas locales.

- **Integraciones de mapas** (runtime-opcionales, guard en `FactionsMod`):
  - **BlueMap**: `de.bluecolored:bluemap-api` (el maven de BlueMap renombró el artefacto
    `BlueMapAPI` a `bluemap-api`; clases idénticas).
  - **squaremap**: el API ya no se publica en ningún maven público, así que se compila
    contra el jar exacto del mod `libs/squaremap-neoforge-mc26.1.2-1.3.13.3.jar`
    (descargado de Modrinth; contiene las mismas clases `xyz.jpenilla.squaremap.api`).
  - **dynmap**: eliminado del port (sin build NeoForge para 26.1). El botón de admin y el
    subcomando `/factions admin reload` se fueron con él. Restaurables desde el original.

- **Objetivo de despliegue**: NeoForge **26.1.2.100** (el rango de dependencia resultante
  `[26.1.2.100,)` cubre también builds superiores como el 26.1.2.114 del MDK).
- Diagnóstico de crash en servidor de producción (pack de ~100 mods): los 10 mixins de
  factions aplicaban correctamente; el `ClassCastException: ArrayList -> AnnotationNode`
  venía de **MixinExtras 0.5.4** (incluido en NeoForge 26.1.2.100) al aplicar un mixin de
  OTRO mod durante la carga de datapacks. NeoForge 26.1.2.114 incluye MixinExtras 0.5.5
  (esa ruta corregida) y con solo factions el arranque es limpio en ambos builds.
  Recomendación: actualizar el servidor a 26.1.2.114+.

## Versiones de Minecraft soportadas (ramas git)

| Rama | MC | NeoForge | Jar |
|---|---|---|---|
| `master` | 26.1.2 | 26.1.2.100+ (rango `[26.1.2.100,)`) | `factions-mc26.1.2-<v>.jar` |
| `mc26.2` | 26.2 | 26.2.0.90+ (rango `[26.2,26.3)`) | `factions-mc26.2-<v>.jar` |
| `mc26.3` | 26.3 | 26.3.0.69-beta+ (rango `[26.3,)`) | `factions-mc26.3-<v>.jar` |

Diferencias entre ramas: solo `gradle.properties` (versiones MC/NeoForge/dependencias)
y los puntos donde 26.2/26.3 cambiaron la API (`TeamColorArgument`/`TeamColor.byName().rgb()`
/ familias `Items.*.pick(DyeColor)`) — las ramas 26.2/26.3 son idénticas entre sí en codigo.
Para corregir un bug: arreglar en `master` y cherry-pick a `mc26.2` y `mc26.3`.
Dependencias por version: placeholder-api-neoforge (3.0.0+26.1 / 3.1.0-beta.1+26.2 /
3.2.0+26.3), squaremap (libs/, por version), TAB (6.0.3 para 26.1.2, 6.2.0 multiloader
para 26.2/26.3), flow-math invariante, bluemap-api 2.8.1 invariante.
