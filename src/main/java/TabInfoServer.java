import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class TabInfoServer extends WebSocketServer {
    private AtomicReference<String> currentTabTitle = new AtomicReference<>("");
    private AtomicReference<String> currentTabUrl = new AtomicReference<>("");
    private volatile boolean running = true;

    public TabInfoServer() throws IOException {
        super(new InetSocketAddress(12345));
        start();
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

    public void stop() {
        running = false;
        try {
            super.stop();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
