let socket = null;

function connectSocket() {
    socket = new WebSocket('ws://localhost:12345');
    
    socket.onopen = function() {
        sendCurrentTab();
    };
    
    socket.onclose = function() {
        setTimeout(connectSocket, 5000);
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
        // Gérer silencieusement les erreurs
    }
}

// Écouter les changements d'onglets
chrome.tabs.onActivated.addListener(sendCurrentTab);
chrome.tabs.onUpdated.addListener(sendCurrentTab);

// Démarrer la connexion WebSocket
connectSocket();
