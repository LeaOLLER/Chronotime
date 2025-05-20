import fi.iki.elonen.NanoHTTPD;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ConfigServer extends NanoHTTPD {
    private static final int PORT = 8000;

    public ConfigServer() throws IOException {
        super(PORT);
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        System.out.println("Serveur de configuration démarré sur le port " + PORT);
    }

    @Override
    public Response serve(IHTTPSession session) {
        Response response;
        
        if (session.getUri().equals("/websocket_port.txt")) {
            try {
                String content = Files.readString(Paths.get("websocket_port.txt"));
                response = newFixedLengthResponse(Response.Status.OK, "text/plain", content);
            } catch (IOException e) {
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
} 