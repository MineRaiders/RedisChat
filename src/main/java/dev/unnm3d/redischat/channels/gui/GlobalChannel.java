package dev.unnm3d.redischat.channels.gui;

import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.api.objects.Channel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;

public class GlobalChannel extends PlayerChannel {
    public GlobalChannel(Channel channel, Player player, boolean isActive) {
        super(channel, player, isActive);
    }

    @Override
    public ItemProvider getItemProvider() {
        ItemStack item;
        if (status == Status.MUTED) {
            item = RedisChat.getInstance().guiSettings.mutedGlobal;
        } else if (status == Status.LISTENING) {
            item = RedisChat.getInstance().guiSettings.activeGlobal;
        } else {
            item = RedisChat.getInstance().guiSettings.idleGlobal;
        }

        final ItemMeta im = item.getItemMeta();
        if (im != null) {
            Component displayComponent = RedisChat.getInstance().getComponentProvider()
                    .parse(player, channel.getDisplayName(), true, false, false);
            String displayName = LegacyComponentSerializer.legacySection().serialize(displayComponent);
            im.setDisplayName(displayName);
        }
        item.setItemMeta(im);
        return new ItemBuilder(item);
    }

}
