# Chronomètre Moderne avec Suivi d'Activité Chrome

Ce projet est un chronomètre moderne qui permet de suivre votre temps passé sur différentes activités, avec une intégration spéciale pour le suivi des onglets Chrome.

## Fonctionnalités

- Chronomètre avec interface moderne
- Catégorisation des sessions (Études, Thales, Lecture, Autre)
- Suivi automatique des onglets Chrome actifs
- Statistiques détaillées par session
- Système de notation des sessions (1-5 étoiles)
- Sauvegarde des tâches accomplies et à faire

## Installation

### 1. Application Java

1. Assurez-vous d'avoir Java 17 ou supérieur installé
2. Clonez le repository
3. Compilez le projet avec Maven :
```bash
mvn clean install
```
4. Lancez l'application :
```bash
mvn javafx:run
```

### 2. Extension Chrome

1. Ouvrez Chrome et allez dans `chrome://extensions/`
2. Activez le "Mode développeur" (en haut à droite)
3. Cliquez sur "Charger l'extension non empaquetée"
4. Sélectionnez le dossier `chrome_tab_tracker_extension` du projet

## Utilisation

### Démarrage

1. Lancez l'application Java
2. Assurez-vous que l'extension Chrome est active
3. L'application affichera automatiquement les URLs des sites visités dans Chrome

### Interface principale

- 🟢 Bouton Play/Pause : Démarre/met en pause le chronomètre
- 🔄 Bouton Reset : Réinitialise le chronomètre
- ✓ Bouton Terminer : Termine la session et enregistre les statistiques
- 📊 Bouton Statistiques : Affiche les statistiques détaillées

### Fin de session

À la fin d'une session, vous pouvez :
- Noter la session (1-5 étoiles)
- Lister les tâches accomplies
- Noter les tâches restantes à faire
- Voir les statistiques de navigation Chrome

### Statistiques

Les statistiques incluent :
- Temps total par session
- Sites web les plus visités
- Temps passé par site
- Historique des sessions

## Configuration technique

### Ports utilisés

- Port 12345 : Communication WebSocket entre l'extension Chrome et l'application

### Structure des fichiers

- `src/main/java/` : Code source Java
- `chrome_tab_tracker_extension/` : Code source de l'extension Chrome
- `target/` : Fichiers compilés

## Dépannage

### L'extension ne se connecte pas

1. Vérifiez que l'application Java est lancée
2. Assurez-vous que le port 12345 est disponible
3. Rechargez l'extension dans Chrome

### Les URLs ne s'affichent pas

1. Vérifiez que l'extension est active dans Chrome
2. Redémarrez l'application Java
3. Vérifiez les logs dans la console de l'extension Chrome

## Contribution

Les contributions sont les bienvenues ! N'hésitez pas à :
- Signaler des bugs
- Proposer des améliorations
- Soumettre des pull requests

## Licence

Ce projet est sous licence MIT. Voir le fichier LICENSE pour plus de détails. # Chronometre
