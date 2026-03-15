package woolbattle.woolbattle.woolsystem;

import com.mongodb.client.MongoDatabase;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bson.Document;
import org.bukkit.*;
import org.bukkit.block.Block;
import woolbattle.woolbattle.MapConfig;
import woolbattle.woolbattle.Main;
import woolbattle.woolbattle.WoolHelper;
import java.util.ArrayList;
import java.util.HashSet;
import static com.mongodb.client.model.Filters.eq;
import static java.lang.String.format;

public class BlockBreakingSystem {

    private static final NamedTextColor green = NamedTextColor.GREEN;
    private static final NamedTextColor blue = NamedTextColor.BLUE;

    private static String mapBlocksId() { return "mapBlocks_" + MapConfig.mapName; }
    private static String mapChunksId() { return "mapChunks_" + MapConfig.mapName; }

    private static ArrayList<Location> mapBlocks = new ArrayList<>();
    private static boolean collectBrokenBlocks = false;
    private static ArrayList<Location> removedBlocks = new ArrayList<>();
    private static ArrayList<Location> placedBlocks = new ArrayList<>();


    //Setter and getter, concerning the previously defined private variables

    public static boolean isCollectBrokenBlocks() {return collectBrokenBlocks;}
    public static void setCollectBrokenBlocks(boolean collectBrokenBlocksArg) {collectBrokenBlocks = collectBrokenBlocksArg;}

    public static ArrayList<Location> getMapBlocks() {return mapBlocks;}
    public static void setMapBlocks(ArrayList<Location> mapBlocksArg) {mapBlocks = mapBlocksArg;}

    public static ArrayList<Location> getRemovedBlocks() {return removedBlocks;}
    public static void setRemovedBlocks(ArrayList<Location> removedBlocks) {BlockBreakingSystem.removedBlocks = removedBlocks;}

    public static void trackPlacedBlock(Location location) {placedBlocks.add(location);}

    public static boolean isPlacedBlock(Location location) {return placedBlocks.contains(location);}

    public static void removePlacedBlock(Location location) {placedBlocks.remove(location);}

    public static void clearPlacedBlocks() {
        for (Location loc : placedBlocks) {
            loc.getBlock().setType(Material.AIR);
        }
        placedBlocks.clear();
    }



    /**
     * Method, dedicated to clearing the mapBlocks array, stored in the specified database.
     * @author Servaturus
     * */
    public static void clearDbMapBlocks(){

        MongoDatabase db = Main.getMongoClient().getDatabase("woolbattle");
        String id = mapBlocksId();
        db.getCollection("map").
                replaceOne(
                eq("_id", id),
                new Document("_id", id).append("mapBlocks", new ArrayList<ArrayList<Double>>())
        );
    }

    /**
     * Method that fetches stored mapBlocks from the db into the cached blocks array.
     * @author Servaturus
     *
     */
    public static void fetchMapBlocks() {
        MongoDatabase db = Main.getMongoClient().getDatabase("woolbattle");
        ArrayList<Location> updatedMapBlocks = mapBlocks;
        String id = mapBlocksId();

        //Checks, whether the "map" collection and the "mapBlocks" documents exist in the db, creates them, if this is not the case.

        if(!db.listCollectionNames().into(new ArrayList<>()).contains("map")){
            db.createCollection("map");
        }
        Document found = db.getCollection("map").find(eq("_id", id)).first();
        if(found == null){
            db.getCollection("map").insertOne( new Document("_id", id).append("mapBlocks", new ArrayList<ArrayList<Double>>()));
        }

        //Iterates over the mapBlocks, present in the db, converts the into valid locations and ultimately add them to a previously created array.


        org.bukkit.World gameWorld = (MapConfig.gameWorldName != null && Bukkit.getWorld(MapConfig.gameWorldName) != null)
                ? Bukkit.getWorld(MapConfig.gameWorldName) : Bukkit.getWorlds().get(0);

        for(ArrayList<Double> argArray: (ArrayList<ArrayList<Double>>) Main.getMongoDatabase().getCollection("map").find(eq("_id", id)).first().get("mapBlocks")){
            if(argArray.size() == 0){
                break;
            }else {
                Location location = new Location(
                        gameWorld,
                        argArray.get(0),
                        argArray.get(1),
                        argArray.get(2)
                );
                if(updatedMapBlocks.contains(location)){
                    continue;
                }
                updatedMapBlocks.add(location);
            }
        }

        //Replaces the currently cached blocks with the previously prepared updated mapBlocks array.

        BlockBreakingSystem.setMapBlocks(updatedMapBlocks);
    }

    /**
     * Method pushing cached mapBlocks towards the specified database.
     * @author Servaturus
     */
    public static void pushMapBlocks(){

        //Pushes the currently present cached blocks into the database.
        if(mapBlocks.size() == 0){

            return;
        }
        String id = mapBlocksId();
        if(!Main.getMongoDatabase().listCollectionNames().into(new ArrayList<>()).contains("map")){
            Main.getMongoDatabase().createCollection("map");
        }
        Document found = Main.getMongoDatabase().getCollection("map").find(eq("_id", id)).first();
        if(found == null){
            Main.getMongoDatabase().getCollection("map").insertOne(new Document("_id", id).append("mapBlocks", new ArrayList<ArrayList<Double>>()));
        }

        //Fetches the stored mapBlocks from the db into a new array (update).
            ArrayList<ArrayList<Double>> update = (ArrayList<ArrayList<Double>>) Main.getMongoDatabase().getCollection("map").find(eq("_id", id)).first().get("mapBlocks");

            //Adds the cached blocks to the updated array, in case they are not already present in said collection.

            for(Location loc : mapBlocks){
                ArrayList<Double> locArray = new ArrayList<Double>(){
                    {
                        add(loc.getX());
                        add(loc.getY());
                        add(loc.getZ());
                    }
                };

                if(!update.contains(locArray)){
                    update.add(locArray);
                }

            }

            //Searches for blocks in the array, about to replace the mapBlocks-array in the db, that are present in the removed-blocks-array and deletes them.

            World gameWorld = (MapConfig.gameWorldName != null && Bukkit.getWorld(MapConfig.gameWorldName) != null)
                    ? Bukkit.getWorld(MapConfig.gameWorldName) : Bukkit.getWorlds().get(0);
            update.removeIf(locArray -> {
                Location loc = new Location(gameWorld, locArray.get(0), locArray.get(1), locArray.get(2));
                return removedBlocks.contains(loc);
            });

            //Replaces the mapBlocksArray in the db with the previously-prepared one (update).
            Main.getMongoDatabase().getCollection("map").replaceOne(eq("_id", id), new Document("_id", id).append("mapBlocks", update));
    }

    /**
     *  Method, meant to convert an array of locations towards an appropriately coloured Component, representing it.
     * @param locs The ArrayList of locations, meant to be converted into a Component.
     * @return The Component, generated according to the input ArrayList of locations.
     */
    public static Component locArrayToComponent(ArrayList<Location> locs){

        Component result = Component.text("[", NamedTextColor.DARK_PURPLE);

        if(locs.size() == 0){
            result = result.append(Component.text("]", NamedTextColor.DARK_PURPLE));
            return result;
        }
        else{

            for(int i = 0; i<locs.size(); i++){

                result = result
                        .append(Component.text("\n", green))
                        .append(Component.text("[", green))
                        .append(Component.text(format("%f,%f,%f",
                                locs.get(i).getX(),
                                locs.get(i).getY(),
                                locs.get(i).getZ()), blue))
                        .append(Component.text("]", green));

                if(i == (locs.size() -1)){
                    result = result.append(Component.text("]", NamedTextColor.DARK_PURPLE));
                }else{
                    result = result.append(Component.text(",", NamedTextColor.AQUA));
                }

            }
        }

        return result;
    }

    /**
     *  Method, meant to convert an array of ArrayList of doubles towards an appropriately coloured Component, representing it.
     * @param locs The ArrayList of locations, meant to be converted into a Component.
     * @return The Component, corresponding with the specified input ArrayList of ArrayLists of doubles.
     */
    public static Component doubleArrArrToComponent(ArrayList<ArrayList<Double>> locs){

        Component result = Component.text("[", NamedTextColor.DARK_PURPLE);

        if(locs.size() == 0){
            result = result.append(Component.text("]", NamedTextColor.DARK_PURPLE));
            return result;
        }
        for(int i = 0; i<locs.size(); i++){

            result = result
                    .append(Component.text("\n", green))
                    .append(Component.text("[", green))
                    .append(Component.text(format("%f,%f,%f",
                            locs.get(i).get(0),
                            locs.get(i).get(1),
                            locs.get(i).get(2)), blue))
                    .append(Component.text("]", green));

            if(i == (locs.size() -1)){
                result = result.append(Component.text("]", NamedTextColor.DARK_PURPLE));
            }else{
                result = result.append(Component.text(",", NamedTextColor.AQUA));
            }

        }
        return result;
    }

    /**
     * Method, capable of adding locations to the local array of map-Blocks, using two input location-vectors.
     * The differences of these vectors in the respective dimensions serve as the height, width and depth of a volume of blocks, whose elements are added to the array of map-blocks.
     * @param a The location, specifying the origin vector of the range, used to add the blocks to the array of map-blocks.
     * @param b The location, specifying the end vector of the range, used to add the blocks to the array of map-blocks.
     * @author Servaturus
     */
    public static void addBlocksByRange(Location a, Location b) {

        World standard = (MapConfig.gameWorldName != null && Bukkit.getWorld(MapConfig.gameWorldName) != null)
                ? Bukkit.getWorld(MapConfig.gameWorldName) : Bukkit.getWorlds().get(0);

        int xdiff = (int) a.getX() - (int) b.getX();
        int ydiff = (int) a.getY() - (int) b.getY();
        int zdiff = (int) a.getZ() - (int) b.getZ();
        ArrayList<Location> locs = new ArrayList<>();
        ArrayList<Integer>
                xs = new ArrayList<>(),
                ys = new ArrayList<>(),
                zs = new ArrayList<>();

        //Adds every x value in the range of xdiff to the xs array.
        if(Integer.signum(xdiff) == 0){
            xs.add((int) b.getX());
        }else{
            for(int i = (int) b.getX(); ((Integer.signum(xdiff)) == -1)? (i>a.getX()) : (i<a.getX()); i += Integer.signum(xdiff)){
                xs.add(i);
            }
        }
        //Similar approach regarding ydiff and ys.
        if(Integer.signum(ydiff) == 0){
            xs.add((int) b.getX());
        }else{
            for(int i = (int) b.getY();((Integer.signum(ydiff)) == -1)? (i>a.getY()) : (i<a.getY()); i += Integer.signum(ydiff)){
                ys.add(i);
            }
        }
        //Another repetition in regard of zdiff and zs.
        if(Integer.signum(zdiff) == 0){
            zs.add((int) b.getZ());
        }else{
            for(int i = (int) b.getZ();((Integer.signum(zdiff)) == -1)? (i>a.getZ()) : (i<a.getZ()); i += Integer.signum(zdiff)){
                zs.add(i);
            }
        }
        //Combines every element of xs with every element of y and the resulting combinations with every element of z.
        for(int x : xs){
            for(int y : ys){
                for(int z : zs){

                    if(WoolHelper.isWool(new Location(standard, x, y, z).getBlock().getType())){
                        locs.add(new Location(standard, x, y, z));
                    }
                }
            }
        }
        //Adds locations, constituted by the former created value pairs (of xs, ys, and zs), to the global mapBlocks array.
        for(Location l : locs){
            if(!mapBlocks.contains(l)){
                mapBlocks.add(l);
            }
        }
    }

    /**
     * Removes any wool blocks in a previously defined range of chunks, not belonging to the blocks of the same map,
     * defined in addition to that.
     * @author Servaturus
     *
     */
    public static void resetMap(){
        Document doc = Main.getMongoDatabase().getCollection("map").find(eq("_id", mapChunksId())).first();
        if(doc == null){
            Main.getInstance().getLogger().warning("There are no chunks, belonging to the map, specified in the database");
            return;
        }

        ArrayList<ArrayList<Long>> mapChunks = (ArrayList<ArrayList<Long>>) doc.get("chunks");
        if(mapChunks == null){
            Main.getInstance().getLogger().warning("Couldn't reset map as there were no chunks defined in the db. Use /map def to specify them.");
            return;
        }
        World world = (MapConfig.gameWorldName != null && Bukkit.getWorld(MapConfig.gameWorldName) != null)
                ? Bukkit.getWorld(MapConfig.gameWorldName) : Bukkit.getWorlds().get(0);
        HashSet<Location> mapBlocksSet = new HashSet<>(mapBlocks);
        for(ArrayList<Long> chunkCoords : mapChunks){
            Chunk chunk = world.getChunkAt( (int) (long) chunkCoords.get(0), (int) (long) chunkCoords.get(1));
            for(int x = 0;x<16;x++){
                for(int y = 0; y< MapConfig.maxHeight; y++){
                    for(int z = 0; z<16;z++){
                        Block block = chunk.getBlock(x,y,z);
                        if(!WoolHelper.isWool(block.getType())){
                            continue;
                        }
                        if(mapBlocksSet.contains(block.getLocation())){
                            continue;
                        }

                        block.setType(Material.AIR);
                    }
                }
            }
        }
    }
}