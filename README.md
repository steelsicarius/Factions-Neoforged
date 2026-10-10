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

- El runtime del sistema en `/usr/java/jdk-25-oracle-aarch64` es una **instalación recortada**
  (solo `java`/`jcmd`/`keytool`; sin `javac` ni `lib/ct.sym`), por lo que **no puede compilar
  con `--release 25`** (ModDevGradle lo necesita para recompilar los artefactos de Minecraft).
- Usar el JDK 25 completo (Temurin, descargado por el auto-provisioning de Gradle):

  ```bash
  JAVA_HOME=~/.gradle/jdks/eclipse_adoptium-25-aarch64-linux.2 ./gradlew build
  ```

- Alternativa a largo plazo: instalar un Oracle JDK 25 completo en el sistema.

- El paquete `git` del sistema desapareció a mitad de sesión; se usa git 2.52.0 de espacio
  de usuario vía `~/.local/bin/git` (wrapper que fija `GIT_EXEC_PATH=~/.git-user/usr/libexec/git-core`,
  RPMs extraídos en `~/git-user`). Con root disponible conviene restaurar el paquete del sistema.
