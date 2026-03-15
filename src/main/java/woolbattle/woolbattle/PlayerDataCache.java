package woolbattle.woolbattle;

import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

import static com.mongodb.client.model.Filters.eq;

public final class PlayerDataCache {

    private static final Map<String, Document> playerPerksCache = new HashMap<>();
    private static final Map<String, Document> playerInventoriesCache = new HashMap<>();

    private PlayerDataCache() {
    }

    public static Document getPlayerPerks(Player player) {
        return player == null ? null : getPlayerPerks(player.getUniqueId().toString());
    }

    public static Document getPlayerPerks(String playerId) {
        if (playerId == null) {
            return null;
        }

        if (playerPerksCache.containsKey(playerId)) {
            return playerPerksCache.get(playerId);
        }

        MongoDatabase db = Main.getMongoDatabase();
        if (db == null) {
            return null;
        }

        Document foundDocument = db.getCollection("playerPerks").find(eq("_id", playerId)).first();
        playerPerksCache.put(playerId, foundDocument);
        return foundDocument;
    }

    public static Document getPlayerInventories(Player player) {
        return player == null ? null : getPlayerInventories(player.getUniqueId().toString());
    }

    public static Document getPlayerInventories(String playerId) {
        if (playerId == null) {
            return null;
        }

        if (playerInventoriesCache.containsKey(playerId)) {
            return playerInventoriesCache.get(playerId);
        }

        MongoDatabase db = Main.getMongoDatabase();
        if (db == null) {
            return null;
        }

        Document foundDocument = db.getCollection("playerInventories").find(eq("_id", playerId)).first();
        playerInventoriesCache.put(playerId, foundDocument);
        return foundDocument;
    }

    public static void putPlayerPerks(Player player, Document document) {
        if (player != null) {
            putPlayerPerks(player.getUniqueId().toString(), document);
        }
    }

    public static void putPlayerPerks(String playerId, Document document) {
        if (playerId != null) {
            playerPerksCache.put(playerId, document);
        }
    }

    public static void putPlayerInventories(Player player, Document document) {
        if (player != null) {
            putPlayerInventories(player.getUniqueId().toString(), document);
        }
    }

    public static void putPlayerInventories(String playerId, Document document) {
        if (playerId != null) {
            playerInventoriesCache.put(playerId, document);
        }
    }

    public static void invalidatePlayer(Player player) {
        if (player == null) {
            return;
        }

        String playerId = player.getUniqueId().toString();
        playerPerksCache.remove(playerId);
        playerInventoriesCache.remove(playerId);
    }

    public static void clear() {
        playerPerksCache.clear();
        playerInventoriesCache.clear();
    }
}
