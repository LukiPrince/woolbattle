package woolbattle.woolbattle.woolsystem;

import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import static java.lang.String.format;

public class BlockRegistrationCommand implements CommandExecutor {
    private final Component syntax = Component.text("\nProper syntax: <blockregistration> <<init/terminate>||range> < || 6*<int> ", NamedTextColor.GREEN);

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(Component.text("The specified number of arguments is too little, than it would be necessary for the" +
                    "command to work properly.", NamedTextColor.RED).append(syntax)
            );
            return false;
        }
        switch (args[0].toLowerCase()) {
            case "init":
                if (!BlockBreakingSystem.isCollectBrokenBlocks()) {
                    BlockBreakingSystem.setCollectBrokenBlocks(true);
                    Bukkit.broadcast(Component.text("The block-scanning-process was successfully initiated.", NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text("The Block-breaking-system is registering the placed blocks already.\n" +
                            "If you want to terminate the scan for newly placed blocks, use the argument", NamedTextColor.RED)
                            .append(Component.text(" terminate", NamedTextColor.GREEN))
                    );
                }

                return false;
            case "terminate":

                if (BlockBreakingSystem.isCollectBrokenBlocks()) {
                    BlockBreakingSystem.setCollectBrokenBlocks(false);
                    Bukkit.broadcast(Component.text("The block-scanning-process was successfully terminated.", NamedTextColor.BLUE));
                } else {
                    sender.sendMessage(Component.text("The Block-breaking-system is currently not registering new blocks, being placed.\n" +
                            "If you want to begin the registration of newly placed blocks, use the argument", NamedTextColor.RED)
                            .append(Component.text(" init", NamedTextColor.GREEN))
                    );
                }
                break;

            case "range":
                if (args.length < 7) {
                    sender.sendMessage(Component.text("The specified number of arguments is too little, than it would be necessary for the" +
                            "command to work properly.", NamedTextColor.RED).append(syntax)
                    );
                    return false;
                }else {
                    try {

                        Location start = new Location(Bukkit.getWorlds().get(0), Double.parseDouble(args[1].toLowerCase()), Double.parseDouble(args[2].toLowerCase()), Double.parseDouble(args[3].toLowerCase()));
                        Location end = new Location(Bukkit.getWorlds().get(0), Double.parseDouble(args[4].toLowerCase()), Double.parseDouble(args[5].toLowerCase()), Double.parseDouble(args[6].toLowerCase()));

                        BlockBreakingSystem.addBlocksByRange(start, end);

                        sender.sendMessage(Component.text("Successfully registered all blocks in the given range. [Only Local - use /mapblocks push to put it in the database]", NamedTextColor.GREEN));

                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
                break;
            default:
                sender.sendMessage(Component.text(args[0], NamedTextColor.DARK_PURPLE)
                        .append(Component.text(" is not a valid argument, concerning this command. ", NamedTextColor.RED))
                        .append(syntax)
                );
        }
        return false;
    }
}