# Chronotime - Chronomètre Moderne avec Analyse de Productivité et Suivi d'Activité Chrome

Application de suivi du temps avec intégration Chrome et rapports Discord automatiques pour analyser votre productivité.


### Suivi du Temps
- **Chronomètre moderne** avec interface JavaFX épurée
- **Catégorisation** : Thales, Études, Perso, ACADOMIA
- **Système de tags** personnalisables par catégorie
- **Notes de productivité** (1-5 étoiles) pour chaque session
- **Suivi automatique** des onglets Chrome actifs

### Analyse de Productivité
- **Jours les plus productifs** : Identifie vos meilleurs jours
- **Créneaux horaires optimaux** : Trouve vos heures de pointe
- **Notes moyennes** par période et catégorie
- **Temps par tag** : Analyse détaillée de vos activités
- **Répartition des notes** : Visualisation de votre productivité

### 🔗 Intégration Discord
- **Rapports hebdomadaires automatiques** (dimanche 20h)
- **Génération manuelle** via bouton dans l'interface
- **Format épuré** sans icônes, facile à lire
- **Statistiques complètes** : temps, productivité, sites web

### Sauvegarde
- **MongoDB Atlas** : Sauvegarde cloud automatique
- **Export Excel** : Données exportables pour analyse
- **Persistance locale** : Tags et configurations

## Installation Rapide

### 1. Prérequis
- **Java 17+** installé
- **Maven 3.6+** pour la compilation
- **Chrome** avec extension (optionnel)

### 2. Lancement Simple
```bash
git clone [votre-repo]
cd Chronotime
./run.sh
```

Le script `run.sh` s'occupe automatiquement de :
- Télécharger JavaFX (si nécessaire)
- Compiler le projet
- Lancer l'application

### 3. Extension Chrome (Optionnel)
Pour le suivi automatique des onglets :

1. Ouvrez `chrome://extensions/`
2. Activez le "Mode développeur"
3. Cliquez "Charger l'extension non empaquetée"
4. Sélectionnez le dossier `chrome_tab_tracker_extension/`

## Utilisation

### Interface Principale
- **▶ Play/Pause** : Démarre/met en pause le chronomètre
- **⟲ Reset** : Remet à zéro le chronomètre
- **✓ Terminer** : Sauvegarde la session avec notes et tâches
- **📊 Statistiques** : Ouvre l'analyse détaillée
- **💬 Discord** : Génère un rapport hebdomadaire immédiat

### Fin de Session
À chaque fin de session, vous pouvez :
- **Noter votre productivité** (1-5 étoiles)
- **Lister les tâches accomplies**
- **Noter ce qui reste à faire**
- **Voir les sites visités** (si extension active)

### Rapports Discord
Les rapports incluent automatiquement :
- **Temps par catégorie** (Thales, Études, etc.)
- **Temps par tag** (dashboard AIS, Veille, etc.)
- **Analyse de productivité** (note moyenne, jour optimal)
- **Top sites visités** avec temps passé
- **Statistiques globales** (temps total, nombre de sessions)

## Exemple de Rapport Discord

```
**RAPPORT HEBDOMADAIRE**
Semaine du 15/07/2025

**TEMPS PAR CATÉGORIE**
**Thales** : 8h15m
**Études** : 3h30m

**TEMPS PAR TAG**
**dashboard AIS** : 4h20m
**Veille** : 2h15m
**TOEIC** : 1h45m

**ANALYSE DE PRODUCTIVITÉ**
Note moyenne : 3.8/5
Répartition des notes : 2★(1) 3★(4) 4★(6) 5★(2)
Jour le plus productif : Mardi (4.2/5)
Créneau le plus productif : 14h-15h (4.1/5)

**TOP SITES VISITÉS**
1. github.com : 2h30m
2. stackoverflow.com : 1h45m
3. chatgpt.com : 1h20m

**STATISTIQUES GLOBALES**
Temps total : 11h45m
Sessions : 13
Tags utilisés : 3
Durée moyenne/session : 0.9h
```

## Configuration Technique

### Ports Utilisés
- **Port 9999** : Serveur de configuration (fixe, pour extension Chrome)
- **Port dynamique** : Serveur WebSocket (sauvegardé dans `websocket_port.txt`)

### Architecture
```
├── src/main/java/           # Code source Java
│   ├── ModernChronometer.java    # Interface principale
│   ├── DiscordReporter.java      # Génération rapports
│   ├── ConfigServer.java         # Serveur configuration
│   ├── TabInfoServer.java        # Serveur WebSocket
│   └── SessionManager.java       # Gestion des données
├── chrome_tab_tracker_extension/ # Extension Chrome
└── run.sh                   # Script de lancement automatique
```

### Base de Données
- **MongoDB Atlas** : Stockage cloud des sessions
- **Format JSON** : Données facilement exportables
- **Backup automatique** : Pas de perte de données

## Dépannage

### L'extension Chrome ne se connecte pas
1. Vérifiez que l'application Java est lancée
2. Rechargez l'extension dans Chrome
3. Consultez la console Chrome (F12) pour les messages

### JavaFX ne se télécharge pas
1. Vérifiez votre connexion internet
2. Supprimez `javafx.zip` et relancez `./run.sh`
3. Installez manuellement depuis [OpenJFX.io](https://openjfx.io/)

### Les rapports Discord ne s'envoient pas
1. Vérifiez votre connexion internet
2. Testez le webhook Discord
3. Consultez les logs de l'application

## Fonctionnalités Avancées

### Tags Intelligents
- **Création dynamique** : Ajoutez des tags à la volée
- **Couleurs automatiques** : Identification visuelle unique
- **Par catégorie** : Tags organisés par domaine d'activité

### Analyse Temporelle
- **Patterns quotidiens** : Identifiez vos heures de pointe
- **Tendances hebdomadaires** : Analysez votre régularité
- **Corrélation durée/productivité** : Optimisez vos sessions

### Export et Partage
- **Export Excel** : Données tabulaires pour analyse
- **Rapports Discord** : Partage automatique avec équipe
- **Sauvegarde cloud** : Données synchronisées

## Optimisation de Productivité

### Conseils d'Utilisation
1. **Notez systématiquement** : Les notes permettent l'analyse
2. **Utilisez les tags** : Granularité dans le suivi
3. **Consultez les rapports** : Identifiez vos patterns
4. **Adaptez vos horaires** : Exploitez vos créneaux optimaux

### Métriques Clés
- **Note moyenne** : Indicateur de productivité global
- **Jour optimal** : Planifiez vos tâches importantes
- **Créneau de pointe** : Concentrez le travail complexe
- **Durée optimale** : Trouvez votre rythme idéal



## 📄 Licence

Ce projet est sous licence MIT. Voir le fichier `LICENSE` pour plus de détails.

---

**Développé avec ❤️ pour optimiser votre productivité**
