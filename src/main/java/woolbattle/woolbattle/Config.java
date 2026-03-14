/**
 MIT License

 Copyright (c) 2022-present SimsumMC, Servaturus and Flashtube

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */

package woolbattle.woolbattle;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;


public class Config {

    /**
     * Global configuration loaded once at startup from plugins/WoolBattle/config.json.
     * Map-specific settings (spawns, world, heights) are in MapConfig.
     * @author SimsumMC & Servaturus
     */

    private static boolean fileExisting = false;

    public static final String mongoDatabase = getString("mongodb");

    /** The map to load on startup. Map-specific settings are in MapConfig. */
    public static final String defaultMap = getString("mapName") != null ? getString("mapName") : "Splend";

    public static final int defaultLives = getInt("defaultLives");
    public static final int spawnProtectionLengthAfterDeath = getInt("spawnProtectionAfterDeath");
    public static final int spawnProtectionLengthAtGameStart = getInt("spawnProtectionAtGameStart");

    public static final int startCooldown = getInt("startCooldown");
    public static final int skipCooldown = getInt("skipCooldown");
    public static final int deathCooldown = getInt("deathCooldown");
    public static final int jumpCooldown = getInt("jumpCooldown");

    public static final int teamSize = getInt("teamSize");

    public static final int givenWoolAmount = getInt("givenWoolAmount");
    public static final int maxStacks = getInt("maxStacks");
    public static final int woolReplaceDelay = getInt("woolReplaceDelay");

    /**
     * Parses plugins/WoolBattle/config.json into a JsonObject.
     * Creates the file with defaults if it does not exist.
     * @author SimsumMC & Servaturus
     */
    private static JsonObject readConfig() {
        if (!fileExisting) {

            // make sure that the directory exists
            File directory = new File("plugins/WoolBattle");
            directory.mkdir();

            File file = new File("plugins/WoolBattle/config.json");
            if (!file.exists()) {
                try {
                    file.createNewFile();
                    Files.write(Paths.get(file.toURI()), Collections.singleton("{\n" +
                            "  \"mongodb\": \"ADD the connection string HERE\",\n" +
                            "  \"mapName\": \"Splend\",\n" +
                            "  \"defaultLives\": 10,\n" +
                            "  \"spawnProtectionAfterDeath\": 5,\n" +
                            "  \"spawnProtectionAtGameStart\": 15,\n" +
                            "  \"startCooldown\": 60,\n" +
                            "  \"skipCooldown\": 30,\n" +
                            "  \"deathCooldown\": 10,\n" +
                            "  \"teamSize\": 2,\n" +
                            "  \"givenWoolAmount\": 1,\n" +
                            "  \"maxStacks\": 3,\n" +
                            "  \"jumpCooldown\": 60,\n" +
                            "  \"woolReplaceDelay\": 10\n" +
                            "}"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                fileExisting = true;
            }
        }
        JsonObject jsonObject = new JsonObject();
        try {
            FileReader fileReader = new FileReader("plugins/WoolBattle/config.json");
            jsonObject = JsonParser.parseReader(fileReader).getAsJsonObject();
        } catch (IOException | JsonSyntaxException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    /**
     * Returns the String value of the given key from plugins/WoolBattle/config.json.
     * @param key the JSON key as a String
     * @author SimsumMC & Servaturus
     */
    public static String getString(String key) {
        JsonObject json = readConfig();
        return json.has(key) ? json.get(key).getAsString() : null;
    }

    /**
     * Returns the int value of the given key from plugins/WoolBattle/config.json.
     * @param key the JSON key as a String
     * @author SimsumMC & Servaturus
     */
    public static int getInt(String key) {
        JsonObject json = readConfig();
        return json.get(key).getAsInt();
    }
}
