let socket = null;
let configPort = null;

async function getConfigPort() {
    try {
        // Essayer d'abord les ports fixes prévisibles
        const fixedPorts = [9999, 9998, 9997, 9996, 9995, 9990];
        
        for (const port of fixedPorts) {
            try {
                const response = await fetch(`http://localhost:${port}/websocket_port.txt`);
                if (response.ok) {
                    configPort = port;
                    console.log('Serveur de configuration trouvé sur le port fixe', port);
                    return port;
                }
            } catch (error) {
                // Continuer avec le port suivant
                continue;
            }
        }
        
        // Essayer les ports communs ensuite
        const commonPorts = [8000, 8001, 8080, 8081, 9000];
        
        for (const port of commonPorts) {
            try {
                const response = await fetch(`http://localhost:${port}/websocket_port.txt`);
                if (response.ok) {
                    configPort = port;
                    console.log('Serveur de configuration trouvé sur le port commun', port);
                    return port;
                }
            } catch (error) {
                // Continuer avec le port suivant
                continue;
            }
        }
        
        // En dernier recours, chercher sur une plage limitée
        console.log('Recherche sur une plage limitée...');
        for (let port = 40000; port < 50000; port += 100) {
            try {
                const response = await fetch(`http://localhost:${port}/websocket_port.txt`, {
                    signal: AbortSignal.timeout(50) // Timeout très rapide
                });
                if (response.ok) {
                    configPort = port;
                    console.log('Serveur de configuration trouvé sur le port', port);
                    return port;
                }
            } catch (error) {
                // Continuer silencieusement
                continue;
            }
        }
        
        throw new Error('Aucun serveur de configuration trouvé');
    } catch (error) {
        console.error('Erreur lors de la recherche du serveur de configuration:', error);
        return null;
    }
}

async function getWebSocketPort() {
    try {
        // Obtenir le port du serveur de configuration s'il n'est pas déjà connu
        if (!configPort) {
            configPort = await getConfigPort();
            if (!configPort) {
                return null;
            }
        }
        
        const response = await fetch(`http://localhost:${configPort}/websocket_port.txt`);
        const port = await response.text();
        return parseInt(port.trim());
    } catch (error) {
        console.error('Erreur lors de la lecture du port WebSocket:', error);
        // Réinitialiser configPort pour forcer une nouvelle recherche
        configPort = null;
        return null;
    }
}

async function connectSocket() {
    const port = await getWebSocketPort();
    if (!port) {
        console.log('Port WebSocket non disponible, nouvelle tentative dans 5 secondes...');
        setTimeout(connectSocket, 5000);
        return;
    }

    socket = new WebSocket(`ws://localhost:${port}`);
    
    socket.onopen = function() {
        console.log('Connecté au serveur WebSocket sur le port', port);
        sendCurrentTab();
    };
    
    socket.onclose = function() {
        console.log('Connexion WebSocket fermée, tentative de reconnexion dans 5 secondes...');
        setTimeout(connectSocket, 5000);
    };

    socket.onerror = function(error) {
        console.error('Erreur WebSocket:', error);
    };
}

async function sendCurrentTab() {
    try {
        const tabs = await chrome.tabs.query({active: true, lastFocusedWindow: true});
        const currentTab = tabs[0];
        
        if (currentTab && socket && socket.readyState === WebSocket.OPEN) {
            const message = `${currentTab.title}|${currentTab.url}`;
            socket.send(message);
        }
    } catch (error) {
        console.error('Erreur lors de l\'envoi des informations de l\'onglet:', error);
    }
}

// Écouter les changements d'onglets
chrome.tabs.onActivated.addListener(() => {
    sendCurrentTab();
});

// Écouter les mises à jour d'onglets
chrome.tabs.onUpdated.addListener(() => {
    sendCurrentTab();
});

// Démarrer la connexion WebSocket
connectSocket();
