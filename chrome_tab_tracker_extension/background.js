let socket = null;

function connectSocket() {
    socket = new WebSocket('ws://localhost:12345');
    
    socket.onopen = function() {
        console.log('Connected to Java application');
        sendCurrentTab();
    };
    
    socket.onclose = function() {
        console.log('Connection closed. Retrying in 5 seconds...');
        setTimeout(connectSocket, 5000);
    };
    
    socket.onerror = function(error) {
        console.log('WebSocket error:', error);
    };
}

async function sendCurrentTab() {
    try {
        const tabs = await chrome.tabs.query({active: true, lastFocusedWindow: true});
        const currentTab = tabs[0];
        
        if (currentTab && socket && socket.readyState === WebSocket.OPEN) {
            console.log('Current tab:', {
                title: currentTab.title,
                url: currentTab.url
            });
            
            // S'assurer que nous avons un titre et une URL valides
            const title = currentTab.title || "Sans titre";
            const url = currentTab.url || "about:blank";
            
            const message = `${title}|${url}`;
            console.log('Sending message:', message);
            socket.send(message);
        } else {
            console.log('Cannot send tab info:', { 
                hasTab: !!currentTab, 
                hasSocket: !!socket, 
                socketState: socket ? socket.readyState : 'no socket',
                tabInfo: currentTab ? {
                    title: currentTab.title,
                    url: currentTab.url
                } : 'no tab'
            });
        }
    } catch (error) {
        console.error('Error getting tab info:', error);
    }
}

// Écouter les changements d'onglets
chrome.tabs.onActivated.addListener(function(activeInfo) {
    console.log('Tab activated:', activeInfo);
    sendCurrentTab();
});

// Écouter les mises à jour d'onglets
chrome.tabs.onUpdated.addListener(function(tabId, changeInfo, tab) {
    console.log('Tab updated:', {
        tabId,
        changeInfo,
        tab: {
            title: tab.title,
            url: tab.url
        }
    });
    
    if (changeInfo.status === 'complete') {
        sendCurrentTab();
    }
});

// Démarrer la connexion WebSocket
connectSocket();

// Envoyer les mises à jour toutes les secondes
setInterval(sendCurrentTab, 1000);
