package woolbattle.woolbattle.storage;

import org.bson.Document;

import java.util.Map;

/**
 * Minimaler Key-Value-Dokumentspeicher. Jede "collection" ist eine Tabelle,
 * jedes Dokument ist per String-_id eindeutig. Ersetzt die fruehere MongoDB-Nutzung.
 */
public interface DocumentStore {

    /** Liefert das Dokument oder null, wenn keines mit dieser id existiert. */
    Document find(String collection, String id);

    /** Fuegt ein Dokument ein (id wird dem Feld "_id" entnommen). Vorhandenes wird ersetzt. */
    void insert(String collection, Document document);

    /** Ersetzt das Dokument mit dieser id vollstaendig (legt es an, falls nicht vorhanden). */
    void replace(String collection, String id, Document document);

    /** Setzt einzelne Felder (Merge). Legt das Dokument an, falls nicht vorhanden. */
    void set(String collection, String id, Map<String, Object> fields);

    /** Fuegt value zur Liste in field hinzu, falls noch nicht enthalten (wie Mongo $addToSet). */
    void addToSet(String collection, String id, String field, Object value);

    void close();
}
