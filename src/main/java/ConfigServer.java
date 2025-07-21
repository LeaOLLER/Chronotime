import fi.iki.elonen.NanoHTTPD;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ConfigServer extends NanoHTTPD {
    private final int port;
    private static final String CONFIG_PORT_FILE = "config_port.txt";

    private static int findAvailablePort() throws IOException {
        // Utiliser un port fixe prévisible pour faciliter la détection par l'extension
        int preferredPort = 9999;
        try (ServerSocket socket = new ServerSocket(preferredPort)) {
            return preferredPort;
        } catch (IOException e) {
            // Si 9999 est occupé, essayer 9998, 9997, etc.
            for (int port = 9998; port >= 9990; port--) {
                try (ServerSocket socket = new ServerSocket(port)) {
                    return port;
                }
                catch (IOException ignored) {
                    // Continuer avec le port suivant
                }
            }
            // En dernier recours, port aléatoire
            try (ServerSocket socket = new ServerSocket(0)) {
                return socket.getLocalPort();
            }
        }
    }

    private void savePortConfig() throws IOException {
        // Sauvegarder le port dans un fichier de configuration
        Files.writeString(Paths.get(CONFIG_PORT_FILE), String.valueOf(port));
        System.out.println("Port du serveur de configuration " + port + " sauvegardé dans " + CONFIG_PORT_FILE);
    }

    public ConfigServer() throws IOException {
        this(findAvailablePort());
    }
    
    private ConfigServer(int port) throws IOException {
        super(port);
        this.port = port;
        
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        System.out.println("Serveur de configuration démarré sur le port " + this.port);
        
        // Sauvegarder le port après le démarrage
        savePortConfig();
    }

    @Override
    public Response serve(IHTTPSession session) {
        System.out.println("ConfigServer - Requête reçue: " + session.getUri());
        
        Response response;
        
        if (session.getUri().equals("/websocket_port.txt")) {
            try {
                String content = Files.readString(Paths.get("websocket_port.txt"));
                System.out.println("ConfigServer - Contenu WebSocket port: " + content);
                response = newFixedLengthResponse(Response.Status.OK, "text/plain", content);
            } catch (IOException e) {
                System.err.println("ConfigServer - Erreur lecture websocket_port.txt: " + e.getMessage());
                response = newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Port non disponible");
            }
        } else {
            response = newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "404 Not Found");
        }

        // Ajouter les en-têtes CORS
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        response.addHeader("Access-Control-Allow-Headers", "Content-Type");
        
        return response;
    }

    @Override
    public void stop() {
        super.stop();
        try {
            // Supprimer le fichier de configuration du port
            Files.deleteIfExists(Paths.get(CONFIG_PORT_FILE));
        } catch (IOException e) {
            // Ignorer les erreurs lors de l'arrêt
        }
    }

    public int getConfigPort() {
        return port;
    }
} 