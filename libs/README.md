# Compile-time dependencies

The source uses compileOnly fileTree(dir: 'libs', include: '*.jar') to compile against the exact Create / Aeronautics / Simulated / Sable APIs used during development.

The binary dependency JARs are intentionally excluded from GitHub. Download the matching files from the official mod/project pages and put them in this folder before building.

The local development setup used these artifacts:

- Create 6.0.10 (NeoForge, Minecraft 1.21.1)
- Create Simulated 1.3.3 (bundled with Create Aeronautics 1.3.3)
- Sable 2.0.6 and its Sable/Rapier runtime library
- Veil 4.5.2
- Flywheel 1.0.6
- Ponder 1.0.82
- Registrate 1.3.0+67
- Sable Companion Common 1.6.0

Some of these libraries are nested in the published dependency mods. The list above documents the local compile classpath; it is not a request to redistribute third-party JARs.
