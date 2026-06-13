package woolbattle.woolbattle.woolsystem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bson.BsonValue;
import org.bson.Document;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import woolbattle.woolbattle.MapConfig;
import woolbattle.woolbattle.Main;
import java.util.ArrayList;
import java.util.Locale;

public class MapBlocksCommand implements CommandExecutor {

    private final Component syntax = Component.text("Proper syntax:\n/mapblocks <fetch/push/ls/ || clear> <[] || db/local", NamedTextColor.GREEN);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if(args.length <1){
            sender.sendMessage(Component.text(
                    "The arguments, added to the command are not portraying\nthe amount of information, needed in order for the command\nto work.\n",
                    NamedTextColor.RED).append(syntax)
            );
            return false;
        }

        else if(args.length>2){
            sender.sendMessage(Component.text(
                    "The amount of arguments, sent to use this command\nhas been to high.\n",
                    NamedTextColor.RED).append(syntax)
            );
            return false;
        }

        else{
            switch(args[0].toLowerCase(Locale.ROOT)){
                case "fetch":
                    sender.sendMessage(Component.text("Initiating fetching process...", NamedTextColor.GREEN));
                    int previousSize = BlockBreakingSystem.getMapBlocks().size();
                    BlockBreakingSystem.fetchMapBlocks();

                    sender.sendMessage(Component.text("In advance of the fetching process, there were ", NamedTextColor.GREEN)
                            .append(Component.text(previousSize, NamedTextColor.BLUE))
                            .append(Component.text(" mapBlocks.\nThe current amount of them is equal to ", NamedTextColor.GREEN))
                            .append(Component.text(BlockBreakingSystem.getMapBlocks().size(), NamedTextColor.BLUE))
                            .append(Component.text(".", NamedTextColor.GREEN))
                    );
                    break;

                case "push":
                    int previousSizeCached = BlockBreakingSystem.getMapBlocks().size();
                    int previousSizeDb;

                    String id = "mapBlocks_" + MapConfig.mapName;
                    Document found = Main.getStore().find("map", id);
                    if(found == null){
                        previousSizeDb = 0;
                        Main.getStore().insert("map", new Document("_id", id).append("mapBlocks", new ArrayList<>()));
                    }
                    /*if(!Main.getMongoClient().listDatabaseNames().into(new ArrayList<String>()).contains("woolbattle")||
                            !Main.getMongoClient().getDatabase("woolbattle").listCollectionNames().into(new ArrayList<String>()).contains("blockBreaking") ||
                            !Main.getMongoClient().getDatabase("woolbattle").getCollection("blockBreaking").listIndexes().into(new ArrayList<Document>()).contains(new Document("_id", "mapBlocks")
                            )){
                        previousSizeDb = 0;

                    }*/
                    else{
                        previousSizeDb = ((ArrayList<BsonValue>) Main.getStore().
                                find("map", id).
                                get("mapBlocks"))
                                .size();
                    }

                    BlockBreakingSystem.pushMapBlocks();

                    int currentSize= ((ArrayList<BsonValue>) Main.getStore().
                            find("map", id).
                            get("mapBlocks")).size();

                    sender.sendMessage(Component.text("The blocks having been pushed are equal to ", NamedTextColor.GREEN)
                            .append(Component.text(previousSizeCached, NamedTextColor.BLUE))
                            .append(Component.text(" .\nThe blocks, stored in the plugin's database in advance of the pushing process were equal to ", NamedTextColor.GREEN))
                            .append(Component.text(previousSizeDb, NamedTextColor.BLUE))
                            .append(Component.text(" .\nThe blocks, present in the database, in this moment are equal to ", NamedTextColor.GREEN))
                            .append(Component.text(currentSize, NamedTextColor.BLUE))
                            .append(Component.text(".", NamedTextColor.GREEN))
                    );
                    break;

                case "ls":
                    if(args.length !=2){
                        sender.sendMessage(Component.text("The amount of arguments specified is not congruent with the one needed. ", NamedTextColor.RED).append(syntax));
                    }
                    switch(args[1].toLowerCase(Locale.ROOT)){
                        case "db":
                            String dbId = "mapBlocks_" + MapConfig.mapName;
                            sender.sendMessage(Component.text("The following array-like string is standing on behalf of the blocks, currently present in the mapBlocks collection of the db:\n", NamedTextColor.GREEN)
                                    .append(BlockBreakingSystem.doubleArrArrToComponent((ArrayList<ArrayList<Double>>) Main.
                                            getStore().
                                            find("map", dbId).
                                            get("mapBlocks")))
                            );
                            break;
                        case "local":
                            sender.sendMessage(Component.text("The following array-like string is standing on behalf of the blocks, currently present in the blockBreakingSystem's Cache:\n", NamedTextColor.GREEN).append(BlockBreakingSystem.locArrayToComponent(BlockBreakingSystem.getMapBlocks())));
                            break;
                    }
                    //sender.sendMessage(Component.text("The following array-like string is standing on behalf of the blocks, currently present in the blockBreakingSystem's Cache:\n", NamedTextColor.GREEN).append(BlockBreakingSystem.locArrayToComponent(BlockBreakingSystem.getMapBlocks())));
                    break;

                case "clear":
                    if(args.length < 2) {
                        sender.sendMessage(Component.text("The arguments, added to the command are not portraying\n" +
                                "the amount of information, needed in order for the command\n" +
                                "to work.\n", NamedTextColor.RED).append(syntax));
                    }
                    else{
                        switch(args[1].toLowerCase(Locale.ROOT)){
                            case "db":
                                sender.sendMessage(Component.text("Clearing the mapBlocks, stored in the db...", NamedTextColor.GREEN));
                                BlockBreakingSystem.clearDbMapBlocks();
                                break;
                            case "local":
                                sender.sendMessage(Component.text("Clearing the cached mapBlocks and removedBlocks array...", NamedTextColor.GREEN));
                                BlockBreakingSystem.setMapBlocks(new ArrayList<Location>());
                                BlockBreakingSystem.setRemovedBlocks(new ArrayList<Location>());
                                break;
                        }
                    }
                    break;
            }
        }
        return false;
    }
}