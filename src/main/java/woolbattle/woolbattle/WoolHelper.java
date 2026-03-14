package woolbattle.woolbattle;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Utility class to handle wool-related Material mappings.
 * Required because in 1.13+ there is no longer a single Material.WOOL with data values.
 * Each wool colour now has its own Material (e.g. RED_WOOL, BLUE_WOOL).
 */
public class WoolHelper {

    /**
     * Returns the specific wool Material for a given DyeColor.
     */
    public static Material getWoolMaterial(DyeColor color) {
        switch (color) {
            case RED:        return Material.RED_WOOL;
            case BLUE:       return Material.BLUE_WOOL;
            case LIME:       return Material.LIME_WOOL;
            case YELLOW:     return Material.YELLOW_WOOL;
            case GREEN:      return Material.GREEN_WOOL;
            case WHITE:      return Material.WHITE_WOOL;
            case ORANGE:     return Material.ORANGE_WOOL;
            case MAGENTA:    return Material.MAGENTA_WOOL;
            case LIGHT_BLUE: return Material.LIGHT_BLUE_WOOL;
            case PINK:       return Material.PINK_WOOL;
            case GRAY:       return Material.GRAY_WOOL;
            case LIGHT_GRAY: return Material.LIGHT_GRAY_WOOL;
            case CYAN:       return Material.CYAN_WOOL;
            case PURPLE:     return Material.PURPLE_WOOL;
            case BROWN:      return Material.BROWN_WOOL;
            case BLACK:      return Material.BLACK_WOOL;
            default:         return Material.WHITE_WOOL;
        }
    }

    /**
     * Returns true if the given Material is any kind of wool.
     */
    public static boolean isWool(Material material) {
        return Tag.WOOL.isTagged(material);
    }

    /**
     * Removes all wool items (of any colour) from the given inventory.
     */
    public static void removeAllWool(Inventory inventory) {
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack != null && isWool(stack.getType())) {
                inventory.setItem(i, null);
            }
        }
    }
}
