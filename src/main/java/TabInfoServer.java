import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicReference;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class TabInfoServer extends WebSocketServer {
    private AtomicReference<String> currentTabTitle = new AtomicReference<>("");
    private AtomicReference<String> currentTabUrl = new AtomicReference<>("");
    private volatile boolean running = true;
    private static final String CONFIG_FILE = "websocket_port.txt";
    private final int port;

    private static int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private void savePortConfig() throws IOException {
        // Sauvegarder le port dans un fichier de configuration
        Files.writeString(Paths.get(CONFIG_FILE), String.valueOf(port));
        System.out.println("Port " + port + " sauvegardé dans " + CONFIG_FILE);
    }

    public TabInfoServer() throws IOException {
        this(findAvailablePort());
    }

    private TabInfoServer(int port) throws IOException {
        super(new InetSocketAddress(port));
        this.port = port;
        
        // Sauvegarder le port pour l'extension Chrome
        savePortConfig();
        
        try {
            start();
            System.out.println("Serveur WebSocket démarré sur le port " + port);
        } catch (Exception e) {
            throw new IOException("Impossible de démarrer le serveur sur le port " + port, e);
        }
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("Nouvelle connexion établie");
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Connexion fermée");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        if (message != null) {
            String[] parts = message.split("\\|");
            if (parts.length >= 2) {
                String title = parts[0].trim();
                String url = parts[parts.length - 1].trim();
                currentTabTitle.set(title);
                currentTabUrl.set(url);
            }
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("Erreur WebSocket : " + ex.getMessage());
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("Serveur WebSocket démarré sur le port " + getPort());
        setConnectionLostTimeout(0);
    }

    public String getCurrentTabTitle() {
        return currentTabTitle.get();
    }

    public String getCurrentTabUrl() {
        return currentTabUrl.get();
    }

    @Override
    public void stop() {
        running = false;
        try {
            // Fermer toutes les connexions actives
            for (WebSocket conn : getConnections()) {
                conn.close();
            }
            // Arrêter le serveur avec un timeout court
            super.stop(500);
            
            // Supprimer le fichier de configuration
            Files.deleteIfExists(Paths.get(CONFIG_FILE));
        } catch (Exception e) {
            // Ignorer les erreurs lors de l'arrêt
        }
    }
}
