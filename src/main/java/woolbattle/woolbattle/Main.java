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
 FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */

package woolbattle.woolbattle;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import woolbattle.woolbattle.achievements.AchievementSystem;
import woolbattle.woolbattle.base.Base;
import woolbattle.woolbattle.lives.LivesSystem;
import woolbattle.woolbattle.lobby.LobbySystem;
import woolbattle.woolbattle.lobby.SetMapCommand;
import woolbattle.woolbattle.lobby.StartGameCommand;
import woolbattle.woolbattle.lobby.StopGameCommand;
import woolbattle.woolbattle.maprestaurationsystem.MapCommand;
import woolbattle.woolbattle.perks.AllActivePerks;
import woolbattle.woolbattle.perks.AllPassivePerks;
import woolbattle.woolbattle.stats.StatsCommand;
import woolbattle.woolbattle.team.TeamSystem;
import woolbattle.woolbattle.woolsystem.BlockBreakingSystem;
import woolbattle.woolbattle.woolsystem.BlockRegistrationCommand;
import woolbattle.woolbattle.woolsystem.MapBlocksCommand;

import org.bukkit.WorldCreator;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;

import static com.mongodb.client.model.Filters.eq;

public final class Main extends JavaPlugin {

    private static Main instance;

    private static MongoClient mongoClient;
    private static MongoDatabase db;

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;

        // Pre-load all map worlds so they are ready when /setmap is used
        new File("plugins/WoolBattle/maps").mkdirs();
        File mapsDir = new File("plugins/WoolBattle/maps");
        File[] mapFiles = mapsDir.listFiles((d, name) -> name.endsWith(".json"));
        if (mapFiles != null) {
            for (File mapFile : mapFiles) {
                try {
                    JsonObject json = JsonParser.parseReader(new FileReader(mapFile)).getAsJsonObject();
                    String gameWorld = json.get("gameWorld").getAsString();
                    if (gameWorld != null && Bukkit.getWorld(gameWorld) == null) {
                        org.bukkit.World loaded = new WorldCreator(gameWorld).createWorld();
                        if (loaded != null) {
                            getLogger().info("[WoolBattle] Pre-loaded world: " + gameWorld);
                        } else {
                            getLogger().warning("[WoolBattle] Failed to load world: " + gameWorld);
                        }
                    }
                } catch (Exception e) {
                    getLogger().warning("[WoolBattle] Could not read map file: " + mapFile.getName());
                }
            }
        }

        // Load map-specific config (spawns, world, heights) for the default map
        MapConfig.load(Config.defaultMap);

        ConnectionString connectionString = new ConnectionString(Config.mongoDatabase);
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .build();
        mongoClient = MongoClients.create(settings);
        db = mongoClient.getDatabase("woolbattle");

        // SimsumMC's Things
        Bukkit.getPluginManager().registerEvents(new LobbySystem(), this);
        Bukkit.getPluginManager().registerEvents(new Base(), this);
        Bukkit.getPluginManager().registerEvents(new AllActivePerks(), this);

        this.getCommand("gstart").setExecutor(new StartGameCommand());
        this.getCommand("gstop").setExecutor(new StopGameCommand());
        this.getCommand("stats").setExecutor(new StatsCommand());
        this.getCommand("setmap").setExecutor(new SetMapCommand());

        AllActivePerks.load();
        AllPassivePerks.load();
        // Beelzebub's Stuff
        Bukkit.getPluginManager().registerEvents(new TeamSystem(), this);
        Bukkit.getPluginManager().registerEvents(new LivesSystem(), this);
        Bukkit.getPluginManager().registerEvents(new AchievementSystem(), this);

        //Servaturus' belongings
        Bukkit.getPluginManager().registerEvents(new Listener(), this);

        getCommand("blockregistration").setExecutor(new BlockRegistrationCommand());
        getCommand("mapblocks").setExecutor(new MapBlocksCommand());
        getCommand("map").setExecutor(new MapCommand());

        Document found = db.getCollection("map").find(eq("_id", "mapBlocks")).first();
        if (found == null) {
            db.getCollection("map").insertOne(new Document("_id", "mapBlocks").append("mapBlocks", new ArrayList<ArrayList<Double>>()));//append("_id", "mapBlocks"));
        }

        BlockBreakingSystem.setCollectBrokenBlocks(false);
        BlockBreakingSystem.fetchMapBlocks();

        // Always daytime in all worlds
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            world.setGameRule(org.bukkit.GameRules.ADVANCE_TIME, false);
            world.setTime(6000);
        }

        for (Player player : Bukkit.getOnlinePlayers())
        {
            player.setAllowFlight(true);
            MongoCollection<Document> collection = db.getCollection("playerAchievements");

            Document foundDocument = collection.find(eq("_id", player.getUniqueId().toString())).first();
            if(foundDocument == null) {
                HashMap<String, Object> playerData = new HashMap<String, Object>() {{
                    put("_id", player.getUniqueId().toString());
                    put("achievements", new ArrayList<String>());
                }};
                Document document = new Document(playerData);
                collection.insertOne(document);
            }
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static Main getInstance(){
        return instance;
    }

    public static MongoDatabase getMongoDatabase() {
        return db;
    }

    public static MongoClient getMongoClient() {
        return mongoClient;
    }
}