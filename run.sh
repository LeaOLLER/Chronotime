#!/bin/bash

# Script de lancement pour Chronotime
echo "🚀 Démarrage de Chronotime..."

# Compilation du projet
echo "📦 Compilation du projet..."
mvn clean compile -q

if [ $? -ne 0 ]; then
    echo "❌ Erreur lors de la compilation"
    exit 1
fi

# Génération du classpath
echo "🔧 Génération du classpath..."
CLASSPATH=$(mvn dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q 2>/dev/null)

if [ $? -ne 0 ] || [ -z "$CLASSPATH" ]; then
    echo "❌ Erreur lors de la génération du classpath"
    exit 1
fi

# Ajout des classes compilées au classpath
FULL_CLASSPATH="target/classes:$CLASSPATH"

# JavaFX local
JAVAFX_PATH="$(pwd)/javafx-sdk-17.0.2/lib"

if [ -d "$JAVAFX_PATH" ] && [ -f "$JAVAFX_PATH/javafx.controls.jar" ]; then
    echo "✅ JavaFX trouvé dans: $JAVAFX_PATH"
    
    # Lancement de l'application avec JavaFX
    echo "🎯 Lancement de l'application..."
    java --module-path "$JAVAFX_PATH" \
         --add-modules javafx.controls,javafx.fxml \
         -cp "$FULL_CLASSPATH" \
         ModernChronometer
    
    # Vérifier le code de sortie
    if [ $? -eq 0 ]; then
        echo "✅ Application fermée normalement"
    else
        echo "❌ Application fermée avec erreur (code: $?)"
    fi
else
    echo "❌ JavaFX non trouvé dans: $JAVAFX_PATH"
    echo "Assurez-vous que JavaFX SDK est correctement extrait."
    exit 1
fi 