# JUSTE-SMP Economy V4 — projet Maven

Plugin Paper pour JUSTE-SMP.

## Pré-requis
- Java 25 (JDK, pas seulement un JRE)
- Maven 3.9+
- Paper 26.3 côté serveur

## Compiler
Depuis ce dossier :

```bash
mvn clean package
```

Le fichier sera généré dans `target/JUSTE-SMP-Economy-4.0.0.jar`.

Puis placer ce JAR dans le dossier `plugins/` du serveur Paper.

## Important
Le build télécharge automatiquement Paper API depuis le dépôt Maven officiel de PaperMC. Une connexion Internet est donc nécessaire au premier build.
