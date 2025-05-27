import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;

public class MongoDBManager {
    // Configuration MongoDB Atlas
    private static final String USERNAME = "Chronuser";
    private static final String PASSWORD = "49184918";
    private static final String CLUSTER_URL = "test.pkhpyvd.mongodb.net";
    private static final String DATABASE_NAME = "chronometer";
    private static final String COLLECTION_NAME = "sessions";
    
    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private final MongoCollection<Document> collection;
    private final Gson gson;

    public MongoDBManager() {
        String connectionString = String.format("mongodb+srv://%s:%s@%s/?retryWrites=true&w=majority&appName=Test",
            USERNAME, PASSWORD, CLUSTER_URL);

        // Configuration optimisée pour le plan gratuit
        ServerApi serverApi = ServerApi.builder()
            .version(ServerApiVersion.V1)
            .build();

        MongoClientSettings settings = MongoClientSettings.builder()
            .applyConnectionString(new ConnectionString(connectionString))
            .serverApi(serverApi)
            .build();

        try {
            this.mongoClient = MongoClients.create(settings);
            this.database = mongoClient.getDatabase(DATABASE_NAME);
            this.collection = database.getCollection(COLLECTION_NAME);
            this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
            
            // Test de connexion
            database.runCommand(new Document("ping", 1));
            System.out.println("Connexion à MongoDB Atlas établie avec succès !");
        } catch (Exception e) {
            System.err.println("Erreur de connexion à MongoDB Atlas : " + e.getMessage());
            throw e;
        }
    }

    public void saveSession(Session session) {
        try {
            String json = gson.toJson(session);
            Document doc = Document.parse(json);
            
            // Utiliser l'ID existant ou en créer un nouveau
            ObjectId id;
            if (session.getMongoId() != null) {
                id = new ObjectId(session.getMongoId());
            } else {
                id = new ObjectId();
                session.setMongoId(id.toString());
            }
            doc.append("_id", id);
            
            collection.replaceOne(
                Filters.eq("_id", id),
                doc,
                new ReplaceOptions().upsert(true)
            );
            System.out.println("Session sauvegardée avec succès dans MongoDB Atlas");
        } catch (Exception e) {
            System.err.println("Erreur lors de la sauvegarde de la session : " + e.getMessage());
        }
    }

    public List<Session> loadAllSessions() {
        List<Session> sessions = new ArrayList<>();
        try (MongoCursor<Document> cursor = collection.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                String mongoId = doc.getObjectId("_id").toString();
                doc.remove("_id"); // Retirer l'ID MongoDB avant la désérialisation
                Session session = gson.fromJson(doc.toJson(), Session.class);
                session.setMongoId(mongoId);
                sessions.add(session);
            }
            System.out.println("Sessions chargées avec succès depuis MongoDB Atlas");
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des sessions : " + e.getMessage());
        }
        return sessions;
    }

    public void deleteSession(Session session) {
        try {
            // Utiliser l'ID MongoDB directement s'il existe
            if (session.getMongoId() != null) {
                collection.deleteOne(Filters.eq("_id", new ObjectId(session.getMongoId())));
            System.out.println("Session supprimée avec succès de MongoDB Atlas");
            } else {
                System.err.println("Impossible de supprimer la session : ID MongoDB manquant");
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la suppression de la session : " + e.getMessage());
        }
    }

    public void close() {
        if (mongoClient != null) {
            try {
                mongoClient.close();
                System.out.println("Connexion MongoDB Atlas fermée avec succès");
            } catch (Exception e) {
                System.err.println("Erreur lors de la fermeture de la connexion : " + e.getMessage());
            }
        }
    }
} 