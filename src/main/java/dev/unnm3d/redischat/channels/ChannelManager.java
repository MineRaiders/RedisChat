package dev.unnm3d.redischat.channels;

import com.github.Anon8281.universalScheduler.UniversalRunnable;
import dev.unnm3d.redischat.Permissions;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.api.DataManager;
import dev.unnm3d.redischat.api.RedisChatAPI;
import dev.unnm3d.redischat.api.VanishIntegration;
import dev.unnm3d.redischat.api.events.AsyncRedisChatMessageEvent;
import dev.unnm3d.redischat.api.objects.*;
import dev.unnm3d.redischat.channels.gui.ChannelGUI;
import dev.unnm3d.redischat.chat.ChatFormat;
import dev.unnm3d.redischat.chat.ComponentProvider;
import dev.unnm3d.redischat.chat.filters.AbstractFilter;
import dev.unnm3d.redischat.chat.filters.FilterManager;
import dev.unnm3d.redischat.chat.filters.FilterResult;
import dev.unnm3d.redischat.mail.MailGUIManager;
import dev.unnm3d.redischat.moderation.MuteManager;
import dev.unnm3d.redischat.settings.Config;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.xenondevs.invui.window.Window;

import java.util.*;
import java.util.regex.Pattern;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ChannelManager extends RedisChatAPI {

    private final RedisChat plugin;
    @Getter
    private final ConcurrentHashMap<String, Channel> registeredChannels;
    private final ConcurrentHashMap<String, String> activePlayerChannels;
    private final ConcurrentHashMap<String, String> worldChannelBindings;
    private Config.WorldChannelSettings worldChannelSettings;
    private final ConcurrentHashMap<String, Channel> localChannels;
    private final Set<String> localWorldChannels;
    @Getter
    private final MuteManager muteManager;
    @Getter
    private final FilterManager filterManager;
    @Getter
    private final ChannelGUI channelGUI;
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    private static final Pattern SOUND_TAG_PATTERN = Pattern.compile("<sound:[^>]+>", Pattern.CASE_INSENSITIVE);


    public ChannelManager(RedisChat plugin) {
        INSTANCE = this;
        this.plugin = plugin;
        this.registeredChannels = new ConcurrentHashMap<>();
        this.activePlayerChannels = new ConcurrentHashMap<>();
        this.worldChannelBindings = new ConcurrentHashMap<>();
        this.localChannels = new ConcurrentHashMap<>();
        this.localWorldChannels = ConcurrentHashMap.newKeySet();
        this.muteManager = new MuteManager(plugin);
        this.filterManager = new FilterManager(plugin);
        this.channelGUI = new ChannelGUI(plugin);

        updateChannels();
    }

    public void updateChannels() {
        plugin.getDataManager().getChannels()
                .thenAccept(channels -> {
                    registeredChannels.clear();
                    if (plugin.config.debug)
                        channels.forEach(ch -> plugin.getLogger().info("Channel: " + ch.getName()));
                    channels.forEach(channel -> registeredChannels.put(channel.getName(), channel));
                    localChannels.forEach(registeredChannels::put);
                });
    }

    @Override
    public ComponentProvider getComponentProvider() {
        return plugin.getComponentProvider();
    }

    @Override
    public Optional<MailGUIManager> getMailManager() {
        return Optional.ofNullable(plugin.getMailGUIManager());
    }

    @Override
    public DataManager getDataManager() {
        return plugin.getDataManager();
    }

    @Override
    public void registerChannel(Channel channel) {
        registeredChannels.put(channel.getName(), channel);
        plugin.getDataManager().registerChannel(channel);
    }

    public void registerLocalChannel(@NotNull Channel channel) {
        localChannels.put(channel.getName(), channel);
        registeredChannels.put(channel.getName(), channel);
    }

    public void unregisterLocalChannel(@NotNull String channelName) {
        localChannels.remove(channelName);
        registeredChannels.remove(channelName);
    }

    public void updateChannel(String channelName, @Nullable Channel channel) {
        if (channel == null) {
            registeredChannels.remove(channelName);
            return;
        }
        registeredChannels.put(channelName, channel);
    }


    @Override
    public void unregisterChannel(String channelName) {
        registeredChannels.remove(channelName);
        plugin.getDataManager().unregisterChannel(channelName);
    }

    @Override
    public void openChannelsGUI(Player player) {
        RedisChat.getScheduler().runTaskAsynchronously(() -> {
            final String channelName = plugin.getChannelManager().getActiveChannel(player.getName());
            Window.Builder.Normal.Single window = Window.single()
                    .setTitle(plugin.guiSettings.channelGUITitle)
                    .setGui(channelGUI.getChannelsGUI(player, channelName == null ? KnownChatEntities.GENERAL_CHANNEL.toString() : channelName))
                    .setCloseHandlers(List.of(() -> new UniversalRunnable() {
                        @Override
                        public void run() {
                            player.updateInventory();
                        }
                    }.runTaskLater(plugin, 1)));

            RedisChat.getScheduler().runTask(() -> window.open(player));
        });
    }

    @Override
    public void broadcastMessage(Channel channel, String message) {
        plugin.getDataManager().sendChatMessage(
                new ChatMessage(
                        new ChannelAudience(),
                        "{message}",
                        message,
                        channel)
        );
    }

    /**
     * Send a message to a specific ChannelAudience
     *
     * @param player         The player that is sending the message
     * @param currentChannel The receiver channel audience of the message
     * @param message        The message to be sent
     */
    public void outgoingMessage(CommandSender player, Channel currentChannel, @NotNull String message) {

        String channelFormat = resolveChannelFormat(player, currentChannel);

        ChatMessage chatMessage = new ChatMessage(
                new ChannelAudience(AudienceType.PLAYER, player.getName()),
                channelFormat,
                message,
                currentChannel
        );

        //Filter and send filter message if present
        final FilterResult result = filterManager.filterMessage(player, chatMessage, AbstractFilter.Direction.OUTGOING);
        chatMessage = result.message();

        if (result.filtered()) {
            result.filteredReason().ifPresent(component ->
                    getComponentProvider().sendComponentOrCache(player, component));
            return;
        }

        final Component formatComponent = getComponentProvider().parseChatMessageFormat(player, chatMessage.getFormat());

        //Parse to MiniMessage component (placeholders, tags and mentions), already parsed in Content filter
        final Component contentComponent = getComponentProvider().parseChatMessageContent(player, chatMessage.getContent());

        //Check if message is empty
        if (PlainTextComponentSerializer.plainText().serialize(contentComponent).trim().isEmpty()) {
            getComponentProvider().sendComponentOrCache(player,
                    getComponentProvider().parse(player,
                            plugin.messages.empty_message, true, false, false));
            return;
        }


        //Call event and check cancellation
        final AsyncRedisChatMessageEvent event = new AsyncRedisChatMessageEvent(player,
                chatMessage.getReceiver(),
                formatComponent,
                contentComponent);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        //Reserialize components
        chatMessage.setFormat(MiniMessage.miniMessage().serialize(event.getFormat()));
        chatMessage.setContent(MiniMessage.miniMessage().serialize(event.getContent()));

        if (!chatMessage.getSender().isDiscord())
            plugin.getDiscordHook().sendDiscordMessage(chatMessage);

        if (currentChannel.getProximityDistance() >= 0) {// Send to local server
            sendGenericChat(chatMessage);
            return;
        }

        plugin.getDataManager().sendChatMessage(chatMessage);
    }

    /**
     * Player chat event, called by the chat listener
     *
     * @param player       Player
     * @param finalMessage The message to be sent
     */
    @Override
    public void outgoingMessage(CommandSender player, @NotNull final String finalMessage) {
        CompletableFuture.runAsync(() -> {
            String channelName = plugin.getChannelManager().getActiveChannel(player.getName());
            Channel audience;
            String message = finalMessage;

            if (plugin.config.enableStaffChat &&
                    player.hasPermission(Permissions.ADMIN_STAFF_CHAT.getPermission()) &&
                    message.startsWith(plugin.config.staffChatPrefix) &&
                    !KnownChatEntities.STAFFCHAT_CHANNEL_NAME.toString().equals(channelName)) {
                message = message.substring(1);
                channelName = KnownChatEntities.STAFFCHAT_CHANNEL_NAME.toString();
            }

            if (channelName == null) {
                audience = getPublicChannel(player);
            } else if (channelName.equals(KnownChatEntities.VOID_CHAT.toString())) {
                getComponentProvider().sendMessage(player, plugin.messages.channelNoPermission);
                return;
            } else {
                audience = getChannel(channelName, player).orElse(getPublicChannel(player));
            }

            if (plugin.config.debug) {
                plugin.getLogger().info("Outgoing message channel: " + audience.getName());
            }

            outgoingMessage(player, audience, message);


        }, plugin.getExecutorService());
    }

    /**
     * Send a private message to a player
     *
     * @param sender       The sender of the message
     * @param receiverName The name of the receiver
     * @param message      The message to be sent
     */
    public void outgoingPrivateMessage(@NotNull CommandSender sender, @NotNull String receiverName, @NotNull String message) {
        final ChatFormat chatFormat = plugin.config.getChatFormat(sender);

        ChatMessage privateChatMessage = new ChatMessage(
                new ChannelAudience(AudienceType.PLAYER, sender.getName()),
                chatFormat.private_format()
                        .replace("%receiver%", receiverName)
                        .replace("%sender%", sender.getName()),
                message,
                new ChannelAudience(AudienceType.PLAYER, receiverName)
        );

        final FilterResult result = filterManager.filterMessage(sender, privateChatMessage, AbstractFilter.Direction.OUTGOING);

        if (result.filtered()) {
            result.filteredReason().ifPresent(component ->
                    getComponentProvider().sendComponentOrCache(sender, component));
            return;
        }
        privateChatMessage = result.message();

        final Component contentComponent = getComponentProvider().parseChatMessageContent(sender, privateChatMessage.getContent());

        //Check if message is empty
        if (PlainTextComponentSerializer.plainText().serialize(contentComponent).trim().isEmpty()) {
            getComponentProvider().sendComponentOrCache(sender,
                    getComponentProvider().parse(sender,
                            plugin.messages.empty_message, true, false, false));
            return;
        }

        //MESSAGE FOR SENDER
        final Component senderFormatComponent = getComponentProvider()
                //Parse format with placeholders
                .parseChatMessageFormat(sender, privateChatMessage.getFormat())
                //Replace {message} with the content component
                .replaceText(builder -> builder.matchLiteral("{message}")
                        .replacement(contentComponent));

        //Send the message to the sender
        plugin.getComponentProvider().sendMessage(sender, senderFormatComponent);

        //MESSAGE FOR RECEIVER
        final Component receiverFormatComponent = getComponentProvider()
                //Parse format with placeholders
                .parseChatMessageFormat(sender, chatFormat.receive_private_format()
                        .replace("%receiver%", receiverName)
                        .replace("%sender%", sender.getName()));

        //Call event and check cancellation
        final AsyncRedisChatMessageEvent event = new AsyncRedisChatMessageEvent(sender,
                privateChatMessage.getReceiver(),
                receiverFormatComponent,
                contentComponent);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        privateChatMessage.setFormat(MiniMessage.miniMessage().serialize(event.getFormat()));
        privateChatMessage.setContent(MiniMessage.miniMessage().serialize(event.getContent()));

        //Send the message to the receiver, the receiver format will be modified on the incoming filter
        plugin.getDataManager().sendChatMessage(privateChatMessage);
    }

    @Override
    public void sendGenericChat(@NotNull ChatMessage chatMessage) {

        Set<Player> recipients;
        if (chatMessage.getReceiver().isPlayer()) {
            final Player pmReceiver = plugin.getServer().getPlayerExact(chatMessage.getReceiver().getName());
            recipients = pmReceiver == null ? Collections.emptySet() : Collections.singleton(pmReceiver);
        } else {
            recipients = new HashSet<>(plugin.getServer().getOnlinePlayers());
        }

        getComponentProvider().logComponent(miniMessage.deserialize(
                chatMessage.getFormat().replace("{message}", chatMessage.getContent())));


        for (Player recipient : recipients) {
            final FilterResult result = filterManager.filterMessage(recipient, chatMessage, AbstractFilter.Direction.INCOMING);
            if (result.filtered()) {
                result.filteredReason().ifPresent(component ->
                        getComponentProvider().sendComponentOrCache(recipient, component));
                continue;
            }

            if (isWorldChannelMismatch(recipient, chatMessage)) {
                continue;
            }

            //If proximity is enabled, check if player is in range
            if (!checkProximity(recipient, chatMessage)) {
                continue;
            }

            //Channel sound
            final Channel soundChannel = getRegisteredChannel(chatMessage.getReceiver().getName()).orElse(getPublicChannel(null));
            if (soundChannel.getNotificationSound() != null) {
                recipient.playSound(recipient.getLocation(), soundChannel.getNotificationSound(), 1, 1);
            }

            //Mention sound
            if (getComponentProvider().purgeTags(chatMessage.getContent()).contains(recipient.getName())) {
                if (!plugin.config.mentionSound.isEmpty()) {
                    final String[] split = plugin.config.mentionSound.split(":");
                    recipient.playSound(recipient.getLocation(), Sound.valueOf(split[0]),
                            Float.parseFloat(split[1]), Float.parseFloat(split[2]));
                }
            }

            //Private message sound
            if (chatMessage.getReceiver().isPlayer()) {
                if (!plugin.config.privateMessageSound.isEmpty()) {
                    final String[] split = plugin.config.privateMessageSound.split(":");
                    recipient.playSound(recipient.getLocation(), Sound.valueOf(split[0]),
                            Float.parseFloat(split[1]), Float.parseFloat(split[2]));
                }
            }

            final ChatMessage messageToSend = result.message();
            String contentToSend = messageToSend.getContent();
            if (shouldStripJoinQuitSound(recipient, messageToSend)) {
                contentToSend = SOUND_TAG_PATTERN.matcher(contentToSend).replaceAll("");
            }
            getComponentProvider().sendComponentOrCache(recipient,
                    miniMessage.deserialize(messageToSend.getFormat().replace("{message}", contentToSend))
            );
        }
        if (chatMessage.getReceiver().isPlayer()) {
            final Component spyComponent = MiniMessage.miniMessage().deserialize(plugin.messages.spychat_format
                            .replace("%receiver%", chatMessage.getReceiver().getName())
                            .replace("%sender%", chatMessage.getSender().getName()))
                    .replaceText(builder -> builder.matchLiteral("{message}")
                            .replacement(MiniMessage.miniMessage().deserialize(chatMessage.getContent())
                            ));

            //Send to spies
            plugin.getServer().getOnlinePlayers().stream()
                    .filter(player -> plugin.getSpyManager().isSpying(player.getName()))
                    .forEach(player -> plugin.getComponentProvider().sendComponentOrCache(player, spyComponent));
        }

    }

    private boolean shouldStripJoinQuitSound(@NotNull CommandSender recipient, @NotNull ChatMessage message) {
        if (recipient.hasPermission(Permissions.JOIN_QUIT_SOUND.getPermission())) {
            return false;
        }
        return message.getReceiver().getPermissions().contains(Permissions.JOIN_QUIT.getPermission());
    }

    private boolean checkProximity(Player recipient, ChatMessage chatMessage) {
        if (chatMessage.getReceiver().getProximityDistance() <= 0) return true;
        if (chatMessage.getSender().isServer()) {
            return true;
        }
        final Optional<? extends Player> sender = plugin.getServer().getOnlinePlayers().stream()
                .filter(p -> p.getName().equals(chatMessage.getSender().getName()))
                .findAny();
        if (sender.isEmpty()) return false;
        if (!sender.get().getWorld().equals(recipient.getWorld())) return false;
        return sender.get().getLocation().distance(recipient.getLocation()) < chatMessage.getReceiver().getProximityDistance();
    }

    private boolean isWorldChannelMismatch(@NotNull Player recipient, @NotNull ChatMessage chatMessage) {
        if (!chatMessage.getReceiver().isChannel()) return false;
        final String channelName = chatMessage.getReceiver().getName();
        final String recipientWorld = recipient.getWorld().getName();
        boolean hasWorldBinding = false;
        for (Map.Entry<String, String> entry : worldChannelBindings.entrySet()) {
            if (!entry.getValue().equalsIgnoreCase(channelName)) continue;
            hasWorldBinding = true;
            if (entry.getKey().equalsIgnoreCase(recipientWorld)) {
                return false;
            }
        }
        return hasWorldBinding;
    }

    private String resolveChannelFormat(@NotNull CommandSender player, @NotNull Channel channel) {
        String channelFormat = channel.getFormat();
        if (worldChannelSettings == null || !worldChannelSettings.enabled()) {
            return channelFormat;
        }
        if (!isWorldChannelName(channel.getName())) {
            return channelFormat;
        }
        if (worldChannelSettings.useWorldFormats()) {
            return wrapChannelFormat(channelFormat, plugin.config.getWorldChatFormat(player).format());
        }
        if (worldChannelSettings.usePlayerFormats()) {
            return wrapChannelFormat(channelFormat, plugin.config.getChatFormat(player).format());
        }
        return channelFormat;
    }

    private String wrapChannelFormat(@NotNull String channelFormat, @NotNull String baseFormat) {
        return channelFormat.contains("{message}")
                ? channelFormat.replace("{message}", baseFormat)
                : baseFormat;
    }

    private boolean isWorldChannelName(@NotNull String channelName) {
        if (localWorldChannels.contains(channelName)) {
            return true;
        }
        for (String value : worldChannelBindings.values()) {
            if (value.equalsIgnoreCase(channelName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void pauseChat(@NotNull Player player) {
        getComponentProvider().pauseChat(player);
    }

    @Override
    public boolean isPaused(@NotNull Player player) {
        return getComponentProvider().isPaused(player);
    }

    @Override
    public void unpauseChat(@NotNull Player player) {
        getComponentProvider().unpauseChat(player);
    }

    @Override
    public Optional<Channel> getChannel(@Nullable String channelName, @Nullable CommandSender player) {
        if (KnownChatEntities.GENERAL_CHANNEL.toString().equals(channelName)) {
            return Optional.of(getPublicChannel(player));
        } else if (KnownChatEntities.STAFFCHAT_CHANNEL_NAME.toString().equals(channelName)) {
            return Optional.of(getStaffChatChannel());
        }
        return getRegisteredChannel(channelName);
    }

    @Override
    public Optional<Channel> getRegisteredChannel(@Nullable String channelName) {
        if (channelName == null) return Optional.empty();
        Channel local = localChannels.get(channelName);
        if (local != null) {
            return Optional.of(local);
        }
        return Optional.ofNullable(registeredChannels.get(channelName));
    }

    @Override
    public Channel getPublicChannel(@Nullable CommandSender player) {
        final Channel publicChannel = getGenericPublic();
        publicChannel.setFormat(plugin.config.getChatFormat(player).format());
        return publicChannel;
    }

    private Channel getGenericPublic() {
        return Channel.builder(KnownChatEntities.GENERAL_CHANNEL.toString())
                .displayName(plugin.guiSettings.publicChannelDisplayName)
                .format(plugin.config.defaultFormat.format())
                .rateLimit(plugin.config.rate_limit)
                .rateLimitPeriod(plugin.config.rate_limit_time_seconds)
                .proximityDistance(plugin.config.publicProximityDistance)
                .discordWebhook(plugin.config.publicDiscordWebhook)
                .filtered(plugin.config.isPublicFiltered)
                .shownByDefault(true)
                .notificationSound(null)
                .build();
    }

    @Override
    public Channel getStaffChatChannel() {
        return Channel.builder(KnownChatEntities.STAFFCHAT_CHANNEL_NAME.toString())
                .displayName(plugin.guiSettings.staffchatChannelDisplayName)
                .format(plugin.config.staffChatFormat)
                .rateLimit(5)
                .rateLimitPeriod(1000)
                .discordWebhook(plugin.config.staffChatDiscordWebhook)
                .filtered(false)
                .shownByDefault(false)
                .notificationSound(null)
                .build();
    }

    @Override
    public void setActiveChannel(String playerName, String channelName) {
        if (localChannels.containsKey(channelName)) {
            updateActiveChannel(playerName, channelName);
            return;
        }
        updateActiveChannel(playerName, channelName);
        plugin.getDataManager().setActivePlayerChannel(playerName, channelName);
    }

    @Override
    public String getActiveChannel(String playerName) {
        return activePlayerChannels.getOrDefault(playerName, KnownChatEntities.GENERAL_CHANNEL.toString());
    }

    public void updateActiveChannel(@NotNull String playerName, @Nullable String channelName) {
        if (plugin.config.debug)
            plugin.getLogger().info("Local active channel for " + playerName + " is now " + channelName);
        if (channelName == null) {
            activePlayerChannels.remove(playerName);
            return;
        }
        activePlayerChannels.put(playerName, channelName);
    }

    @Override
    public void addVanishIntegration(VanishIntegration vanishIntegration) {
        plugin.getPlayerListManager().addVanishIntegration(vanishIntegration);
    }

    @Override
    public void removeVanishIntegration(VanishIntegration vanishIntegration) {
        plugin.getPlayerListManager().removeVanishIntegration(vanishIntegration);
    }

    @Override
    public void setWorldChannel(@NotNull String worldName, @NotNull Channel channel) {
        registerChannel(channel);
        worldChannelBindings.put(normalizeWorldName(worldName), channel.getName());
    }

    @Override
    public void setWorldChannel(@NotNull String worldName, @NotNull String channelName) {
        worldChannelBindings.put(normalizeWorldName(worldName), channelName);
    }

    @Override
    public Optional<String> getWorldChannel(@NotNull String worldName) {
        return Optional.ofNullable(worldChannelBindings.get(normalizeWorldName(worldName)));
    }

    @Override
    public void clearWorldChannel(@NotNull String worldName) {
        worldChannelBindings.remove(normalizeWorldName(worldName));
    }

    public void applyWorldChannel(@NotNull Player player) {
        ensureAutoCreatedWorldChannel(player.getWorld().getName());
        getWorldChannel(player.getWorld().getName()).ifPresent(channelName -> {
            if (getChannel(channelName, player).isEmpty()) {
                if (plugin.config.debug) {
                    plugin.getLogger().warning("World channel bound to missing channel: " + channelName);
                }
                return;
            }
            setActiveChannel(player.getName(), channelName);
        });
    }

    private String normalizeWorldName(@NotNull String worldName) {
        return worldName.toLowerCase(Locale.ROOT);
    }

    public void applyWorldChannelConfig(@Nullable Config.WorldChannelSettings settings) {
        worldChannelBindings.clear();
        this.worldChannelSettings = settings;
        localWorldChannels.forEach(this::unregisterLocalChannel);
        localWorldChannels.clear();
        if (settings == null || !settings.enabled()) {
            return;
        }

        Map<String, String> bindings = settings.bindings();
        if (bindings == null) {
            bindings = Map.of();
        }

        Config.WorldChannelTemplate template = settings.template();
        for (Map.Entry<String, String> entry : bindings.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            final String worldName = entry.getKey();
            final String channelName = entry.getValue().trim();
            if (channelName.isEmpty()) {
                continue;
            }

            if (settings.autoCreate()) {
                boolean isDefaultChannel = KnownChatEntities.GENERAL_CHANNEL.toString().equalsIgnoreCase(channelName)
                        || KnownChatEntities.STAFFCHAT_CHANNEL_NAME.toString().equalsIgnoreCase(channelName);
                if (!isDefaultChannel && getRegisteredChannel(channelName).isEmpty()) {
                    createChannelFromTemplate(settings, template, worldName, channelName);
                }
            }

            worldChannelBindings.put(normalizeWorldName(worldName), channelName);
        }

        if (settings.autoCreate() && settings.autoCreatePrefixes() != null && !settings.autoCreatePrefixes().isEmpty()) {
            plugin.getServer().getWorlds().forEach(world -> ensureAutoCreatedWorldChannel(world.getName()));
        }
    }

    private void createChannelFromTemplate(@NotNull Config.@Nullable WorldChannelSettings settings, Config.WorldChannelTemplate template, String worldName, String channelName) {
        Config.WorldChannelTemplate activeTemplate = template != null ? template : new Config.WorldChannelTemplate(
                "<gold>[%world%]</gold>",
                "<gray>[%world%]</gray> {message}",
                5,
                3,
                true,
                true,
                false,
                ""
        );
        String displayName = activeTemplate.displayName()
                .replace("%world%", worldName)
                .replace("%channel%", channelName);
        String format = activeTemplate.format()
                .replace("%world%", worldName)
                .replace("%channel%", channelName);
        String notificationSound = activeTemplate.notificationSound();
        if (notificationSound != null && notificationSound.isBlank()) {
            notificationSound = null;
        }
        Channel channel = Channel.builder(channelName)
                .displayName(displayName)
                .format(format)
                .rateLimit(activeTemplate.rateLimit())
                .rateLimitPeriod(activeTemplate.rateLimitPeriod())
                .filtered(activeTemplate.filtered())
                .shownByDefault(activeTemplate.shownByDefault())
                .permissionEnabled(activeTemplate.permissionEnabled())
                .notificationSound(notificationSound)
                .build();
        if (settings.localOnly()) {
            registerLocalChannel(channel);
            localWorldChannels.add(channelName);
        } else {
            registerChannel(channel);
        }
    }

    private boolean ensureAutoCreatedWorldChannel(@NotNull String worldName) {
        if (worldChannelSettings == null || !worldChannelSettings.enabled()) {
            return false;
        }
        String normalizedWorld = normalizeWorldName(worldName);
        if (worldChannelBindings.containsKey(normalizedWorld)) {
            return true;
        }
        if (!worldChannelSettings.autoCreate()) {
            return false;
        }

        List<String> prefixes = worldChannelSettings.autoCreatePrefixes();
        if (prefixes == null || prefixes.isEmpty()) {
            return false;
        }

        boolean matches = prefixes.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(prefix -> !prefix.isEmpty())
                .map(prefix -> prefix.contains("*") ? prefix.substring(0, prefix.indexOf('*')) : prefix)
                .anyMatch(prefix -> normalizedWorld.startsWith(prefix.toLowerCase(Locale.ROOT)));

        if (!matches) {
            return false;
        }

        String channelName = worldName;
        if (!KnownChatEntities.GENERAL_CHANNEL.toString().equalsIgnoreCase(channelName)
                && !KnownChatEntities.STAFFCHAT_CHANNEL_NAME.toString().equalsIgnoreCase(channelName)) {
            if (getRegisteredChannel(channelName).isEmpty()) {
                Config.WorldChannelTemplate template = worldChannelSettings.template();
                createChannelFromTemplate(worldChannelSettings, template, worldName, channelName);
            }
        }

        worldChannelBindings.put(normalizeWorldName(worldName), channelName);
        return true;
    }

    public List<Channel> getAllChannels() {
        List<Channel> channels = new ArrayList<>();
        channels.add(getStaffChatChannel());
        Map<String, Channel> all = new LinkedHashMap<>(registeredChannels);
        all.putAll(localChannels);
        channels.addAll(all.values());
        return channels;
    }

}
