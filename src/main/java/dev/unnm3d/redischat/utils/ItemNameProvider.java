package dev.unnm3d.redischat.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public class ItemNameProvider {
    private Method getItemNameMethod;
    private Method hasItemNameMethod;
    private final boolean useItemName;

    public ItemNameProvider(boolean useItemName) {
        try {
            this.getItemNameMethod = ItemMeta.class.getDeclaredMethod("getItemName");
            this.hasItemNameMethod = ItemMeta.class.getDeclaredMethod("hasItemName");
        } catch (NoSuchMethodException ignored) {
            this.useItemName = false;
            Bukkit.getLogger().warning("Failed to find ItemMeta#getItemName() method. Falling back to display name.");
            return;
        }
        this.useItemName = useItemName;
    }

    public String getItemName(ItemStack itemStack) {
        final ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            return "";
        }
        if (getItemNameMethod == null || !useItemName) {
            return itemMeta.getDisplayName();
        }
        try {
            Object value = getItemNameMethod.invoke(itemMeta);
            return switch (value) {
                case null -> itemMeta.getDisplayName();
                case Component component -> LegacyComponentSerializer.legacySection().serialize(component);
                case String string -> string;
                default -> value.toString();
            };
        } catch (IllegalAccessException | InvocationTargetException e) {
            return itemMeta.getDisplayName();
        }
    }

    public boolean hasItemName(ItemStack itemMeta) {
        if (itemMeta.getItemMeta() == null) {
            return false;
        }
        if (hasItemNameMethod == null || !useItemName)
            return itemMeta.getItemMeta().hasDisplayName();
        try {
            Object value = hasItemNameMethod.invoke(itemMeta.getItemMeta());
            return value instanceof Boolean bool ? bool : itemMeta.getItemMeta().hasDisplayName();
        } catch (IllegalAccessException | InvocationTargetException e) {
            return itemMeta.getItemMeta().hasDisplayName();
        }
    }
}
