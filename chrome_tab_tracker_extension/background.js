let socket = null;

async function getWebSocketPort() {
    try {
        const response = await fetch('http://localhost:8000/websocket_port.txt');
        const port = await response.text();
        return parseInt(port.trim());
    } catch (error) {
        console.error('Erreur lors de la lecture du port:', error);
        return null;
    }
}

async function connectSocket() {
    const port = await getWebSocketPort();
    if (!port) {
        console.log('Port non disponible, nouvelle tentative dans 5 secondes...');
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

chrome.tabs.onUpdated.addListener((tabId, changeInfo, tab) => {
    if (changeInfo.status === 'complete') {
        sendCurrentTab();
    }
});

// Démarrer la connexion WebSocket
connectSocket();
