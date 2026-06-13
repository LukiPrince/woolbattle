package woolbattle.woolbattle.storage;

import org.bson.Document;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SQLite-Implementierung des DocumentStore. Eine Tabelle pro Collection,
 * Dokument als RELAXED-JSON in der Spalte "data". Methoden sind synchronized,
 * weil eine einzelne Connection geteilt wird.
 */
public final class SqliteDocumentStore implements DocumentStore {

    private static final String[] COLLECTIONS = {
            "playerPerks", "playerInventories", "playerAchievements",
            "playerStats", "map", "blockBreaking"
    };

    private static final JsonWriterSettings JSON =
            JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build();

    private final Connection connection;

    public SqliteDocumentStore(String path) {
        try {
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            initSchema();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to open SQLite database at " + path, e);
        }
    }

    private void initSchema() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL");
            st.execute("PRAGMA synchronous=NORMAL");
            for (String c : COLLECTIONS) {
                st.execute("CREATE TABLE IF NOT EXISTS \"" + c +
                        "\" (_id TEXT PRIMARY KEY, data TEXT NOT NULL)");
            }
        }
    }

    @Override
    public synchronized Document find(String collection, String id) {
        String sql = "SELECT data FROM \"" + collection + "\" WHERE _id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Document.parse(rs.getString(1));
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("find failed on " + collection + "/" + id, e);
        }
    }

    @Override
    public synchronized void insert(String collection, Document document) {
        upsert(collection, document.get("_id").toString(), document);
    }

    @Override
    public synchronized void replace(String collection, String id, Document document) {
        upsert(collection, id, document);
    }

    @Override
    public synchronized void set(String collection, String id, Map<String, Object> fields) {
        Document doc = find(collection, id);
        if (doc == null) {
            doc = new Document("_id", id);
        }
        doc.putAll(fields);
        upsert(collection, id, doc);
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized void addToSet(String collection, String id, String field, Object value) {
        Document doc = find(collection, id);
        if (doc == null) {
            doc = new Document("_id", id);
        }
        Object current = doc.get(field);
        List<Object> list = (current instanceof List)
                ? new ArrayList<>((List<Object>) current)
                : new ArrayList<>();
        if (!list.contains(value)) {
            list.add(value);
        }
        doc.put(field, list);
        upsert(collection, id, doc);
    }

    private void upsert(String collection, String id, Document document) {
        String sql = "INSERT INTO \"" + collection + "\" (_id, data) VALUES (?, ?) " +
                "ON CONFLICT(_id) DO UPDATE SET data = excluded.data";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, document.toJson(JSON));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("upsert failed on " + collection + "/" + id, e);
        }
    }

    @Override
    public synchronized void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            // ignore on shutdown
        }
    }
}
