package woolbattle.woolbattle.storage;

import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class SqliteDocumentStoreTest {

    private SqliteDocumentStore store;

    @BeforeEach
    void setUp(@TempDir Path dir) {
        store = new SqliteDocumentStore(dir.resolve("test.db").toString());
    }

    @AfterEach
    void tearDown() {
        store.close();
    }

    @Test
    void findReturnsNullWhenMissing() {
        assertNull(store.find("playerStats", "nope"));
    }

    @Test
    void insertThenFindRoundTrips() {
        Document doc = new Document("_id", "p1").append("wins", 5).append("kills", 10);
        store.insert("playerStats", doc);
        Document found = store.find("playerStats", "p1");
        assertNotNull(found);
        assertEquals(5, found.get("wins"));
        assertEquals(10, found.get("kills"));
    }

    @Test
    void replaceOverwritesDocument() {
        store.insert("playerStats", new Document("_id", "p1").append("wins", 1));
        store.replace("playerStats", "p1", new Document("_id", "p1").append("wins", 99));
        assertEquals(99, store.find("playerStats", "p1").get("wins"));
    }

    @Test
    void setUpdatesFieldsAndCreatesWhenMissing() {
        store.set("playerStats", "p1", Map.of("wins", 3, "games", 7));
        Document found = store.find("playerStats", "p1");
        assertEquals(3, found.get("wins"));
        assertEquals(7, found.get("games"));
        assertEquals("p1", found.get("_id"));
    }

    @Test
    void addToSetAppendsOnceAndDeduplicates() {
        store.addToSet("playerAchievements", "p1", "achievements", "fullwool");
        store.addToSet("playerAchievements", "p1", "achievements", "fullwool");
        store.addToSet("playerAchievements", "p1", "achievements", "killstreak5");
        List<?> list = (List<?>) store.find("playerAchievements", "p1").get("achievements");
        assertEquals(List.of("fullwool", "killstreak5"), list);
    }

    @Test
    void preservesNestedDoubleArrays() {
        List<List<Double>> blocks = new ArrayList<>();
        blocks.add(List.of(-37.0, 89.0, -6.0));
        store.insert("map", new Document("_id", "mapBlocks").append("mapBlocks", blocks));
        Document found = store.find("map", "mapBlocks");
        List<?> outer = (List<?>) found.get("mapBlocks");
        List<?> inner = (List<?>) outer.get(0);
        assertEquals(-37.0, inner.get(0));
        assertEquals(89.0, inner.get(1));
    }
}
