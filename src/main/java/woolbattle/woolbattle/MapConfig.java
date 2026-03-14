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

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.WorldCreator;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Holds all map-specific configuration values that can change at runtime
 * when a different map is selected via /setmap.
 */
public class MapConfig {

    public static String mapName;
    public static String gameWorldName;
    public static int maxHeight;
    public static int minHeight;

    public static Location lobbyLocation;
    public static Location midLocation;
    public static Location blueLocation;
    public static Location redLocation;
    public static Location greenLocation;
    public static Location yellowLocation;

    /**
     * Loads the map-specific config from plugins/WoolBattle/maps/<name>.json
     * and updates all static fields accordingly.
     * @param name the map name (matches the filename without .json)
     * @return true if the map was loaded successfully
     */
    public static boolean load(String name) {
        File file = new File("plugins/WoolBattle/maps/" + name + ".json");
        if (!file.exists()) {
            Bukkit.getLogger().severe("[WoolBattle] Map config not found: " + file.getPath());
            return false;
        }

        JsonObject json;
        try {
            json = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();
        } catch (IOException | JsonSyntaxException e) {
            e.printStackTrace();
            return false;
        }

        mapName = name;
        gameWorldName = json.get("gameWorld").getAsString();
        maxHeight = json.get("maxHeight").getAsInt();
        minHeight = json.get("minHeight").getAsInt();

        // Load the game world if it isn't already loaded
        if (gameWorldName != null && Bukkit.getWorld(gameWorldName) == null) {
            new WorldCreator(gameWorldName).createWorld();
        }

        // Lobby is always in the first loaded world (the lobby world)
        org.bukkit.World lobbyWorld = Bukkit.getWorlds().get(0);

        org.bukkit.World gameWorld = (gameWorldName != null && Bukkit.getWorld(gameWorldName) != null)
                ? Bukkit.getWorld(gameWorldName)
                : Bukkit.getWorlds().get(0);

        JsonArray lobbyCoords = json.getAsJsonArray("lobbySpawn");
        lobbyLocation = new Location(lobbyWorld,
                lobbyCoords.get(0).getAsLong(), lobbyCoords.get(1).getAsLong(), lobbyCoords.get(2).getAsLong());

        JsonArray midCoords = json.getAsJsonArray("mapSpawn");
        midLocation = new Location(gameWorld,
                midCoords.get(0).getAsLong(), midCoords.get(1).getAsLong(), midCoords.get(2).getAsLong());

        JsonArray teamCoords = json.getAsJsonArray("teamSpawns");
        blueLocation   = toLocation(gameWorld, teamCoords.get(0).getAsJsonArray());
        redLocation    = toLocation(gameWorld, teamCoords.get(1).getAsJsonArray());
        greenLocation  = toLocation(gameWorld, teamCoords.get(2).getAsJsonArray());
        yellowLocation = toLocation(gameWorld, teamCoords.get(3).getAsJsonArray());

        return true;
    }

    private static Location toLocation(org.bukkit.World world, JsonArray coords) {
        return new Location(world, coords.get(0).getAsLong(), coords.get(1).getAsLong(), coords.get(2).getAsLong());
    }
}
