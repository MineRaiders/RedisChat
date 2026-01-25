package dev.unnm3d.redischat.integrations;

import dev.unnm3d.redischat.api.VanishIntegration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.sayandev.sayanvanish.api.SayanVanishAPI;
import org.sayandev.sayanvanish.api.User;

public class SayanVanishIntegration implements VanishIntegration {

    private final SayanVanishAPI<User> sayanVanishAPI;

    public SayanVanishIntegration() {
        this.sayanVanishAPI = SayanVanishAPI.getInstance();
    }

    @Override
    public boolean canSee(CommandSender viewer, String playerName) {
        // Viewers with vanish permission can always see
        if (viewer.hasPermission("sayanvanish.vanish.use")) {
            return true;
        }

        // Check if the player is vanished
        return sayanVanishAPI.getVanishedUsers().stream()
                .noneMatch(user -> user.getUsername().equalsIgnoreCase(playerName));
    }

    @Override
    public boolean isVanished(Player player) {
        return player != null && sayanVanishAPI.isVanished(player.getUniqueId());
    }
}
