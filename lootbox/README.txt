PLUGIN LOOTBOX HORAIRE - FALIX / SPIGOT 26.3

FONCTIONNALITES
- /lootbox (alias /lb) : récupère une récompense aléatoire une fois par heure.
- Le cooldown est individuel et persiste après un redémarrage.
- Si l'inventaire est plein, la récompense tombe au sol.
- Le spawner obtenu est configuré pour faire apparaître des creepers.
- /lootbox reset <pseudo> : réinitialise le cooldown d'un joueur connecté (op).

PROBABILITES
0,5 % : 1 Elytra
7 % : 1 lingot de Netherite
20 % : 5 diamants
2,5 % : 1 spawner à Creeper
40 % : 5 pommes dorées
10 % : 1 pomme dorée enchantée (pomme de Notch)
20 % : 1 totem d'immortalité
Total : 100 %

COMPILATION
Prérequis : JDK 25 (requis par Minecraft/Spigot 26.3) et Maven.
Dans le dossier du projet, lancer : mvn clean package
Le fichier généré sera : target/lootbox-horaire-1.0.0.jar

INSTALLATION SUR FALIX
1. Arrête le serveur.
2. Dépose le .jar dans le dossier plugins.
3. Redémarre le serveur.
4. En jeu, tape /lootbox.

NOTE : le code est compilé en bytecode Java 21 pour rester compatible avec le runtime Java 25.
Le plugin utilise uniquement l'API Spigot/Bukkit.
