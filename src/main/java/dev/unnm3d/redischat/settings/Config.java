package dev.unnm3d.redischat.settings;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import dev.unnm3d.redischat.chat.ChatFormat;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

@Configuration
public final class Config implements ConfigValidator {

    @Comment({"RedisChat storage type, can be REDIS , MySQL+PM or SQLITE+PM (PM means PluginMessages)",
            "If you use Mysql you need a proxy. The plugin will send the data to the proxy via pluginmessages",
            "If you use REDIS you don't need any proxy, THIS IS THE RECOMMENDED AND MOST EFFICIENT OPTION"})
    public String dataMedium = DataType.REDIS.keyName;
    @Comment("Leave password or user empty if you don't have a password or user")
    public RedisSettings redis = new RedisSettings("localhost",
            6379,
            "",
            "",
            0,
            1000,
            "RedisChat",
            3);
    public Mysql mysql = new Mysql("127.0.0.1",
            3306,
            "redischat",
            "com.mysql.cj.jdbc.Driver",
            "?autoReconnect=true"
                    + "&useSSL=false"
                    + "&useUnicode=true"
                    + "&characterEncoding=UTF-8",
            "root",
            "password",
            5,
            5,
            1800000,
            30000,
            20000);
    @Comment({"The cluster id, if you have multiple servers you need to set a different id for each group of servers",
            "Doesn't work completely if you're using something different than redis"})
    public String clusterId = "0";
    @Comment("Webeditor URL")
    public String webEditorUrl = "https://webui.advntr.dev/";
    @Comment("Enables /rmail /mail and the whole feature")
    public boolean enableMails = true;
    @Comment({"Use RedisChat for join and quit messages",
            "The quit message will be delayed because it might be a early reconnection to one of the servers using RedisChat"})
    public boolean enableQuitJoinMessages = true;
    @Comment("If true, join/quit messages are only sent on the local server (no Redis broadcast)")
    public boolean joinQuitLocalOnly = false;
    @Comment("If true, RedisChat will log public chat messages")
    public boolean chatLogging = false;
    @Comment({"Here you can decide your chat format", "Permission format is overridden on descending order", "(if a player has default and vip, if default is the first element, vip will be ignored)"})
    public List<ChatFormat> formats = List.of(
            new ChatFormat("redischat.staff",
                    "{time} {ignorebtn} {mailbtn} {player} <dark_gray>» <gold>{message}",
                    "<white>✉<green>⬆</green></white> <dark_aqua>MSG <grey>(Me ➺ <green>%receiver%<grey>): <white>{message}",
                    "<white>✉<green>⬇</green></white> <dark_aqua>MSG <grey>(<green>%sender%<grey> ➺ Me): <white>{message}",
                    "<aqua>@%player% (staff)</aqua>",
                    "<aqua><click:open_url:'%link%'>%domain% or %path%</click></aqua>",
                    "<green>%player_name% joined the server",
                    "<red>%player_name% is no longer online"),
            new ChatFormat("redischat.default",
                    "{time} {ignorebtn} {mailbtn} {player} <dark_gray>» %redischat_chat_color%{message}",
                    "<white>✉<green>⬆</green></white> <dark_aqua>MSG <grey>(Me ➺ <green>%receiver%<grey>): <white>{message}",
                    "<white>✉<green>⬇</green></white> <dark_aqua>MSG <grey>(<green>%sender%<grey> ➺ Me): <white>{message}",
                    "<aqua>@%player%</aqua>",
                    "<aqua><click:open_url:'%link%'>[Open web page <red>(be careful)</red>]</click></aqua>",
                    "<green>%player_name% joined the server",
                    "<red>%player_name% is no longer online")
    );

    @Comment("Fallback format if the player doesn't have any of the formats above")
    public ChatFormat defaultFormat = new ChatFormat("none",
            "No format » <reset><gray>{message}",
            "No format: Me ➺ <green>%receiver%<grey> : <white>{message}",
            "No format: <green>%sender%<grey> ➺ Me : <white>{message}",
            "<red>@%player%</red>",
            "No format: <aqua><click:open_url:'%link%'>[Open web page <red>(be careful)</red>]</click></aqua>",
            "No format: %player_name% joined the server",
            "No format: %player_name% is no longer online"
    );

    @Comment({"Here you can create your Minimessage Components for chat formats", "You can give them an identifier, which will go under the format {}"})
    public Map<String, String> components = new TreeMap<>(Map.ofEntries(
            Map.entry("ignorebtn", "<click:run_command:/ignore %player_name%><hover:show_text:'<red>Ignore %player_name%</red>'>[<red>✖</red>]</click>"),
            Map.entry("mailbtn", "<click:suggest_command:/mail send %player_name%><hover:show_text:'<green>Send a mail to %player_name%</green>'>[<green>✉</green>]</click>"),
            Map.entry("player", "<click:suggest_command:/msg %player_name%><hover:show_text:'<gray>Info" +
                    "|</gray> <white>%player_displayname%</white> <br>↪ <gold>Money</gold>: <white>%vault_eco_balance%$</white>" +
                    "<br>↪ <green>Server</green>: <white>%server_name%</white> <br><br><gray>Click" +
                    "to send a private message</gray>'>%vault_prefix% %player_name%</click>"),
            Map.entry("time", "[<gray>%localtime_time_HH:mm%</gray>]")
    ));

    @Comment({"Here you can create your own placeholders", "You can give them an identifier, which will go under the format <>", "You can give them actions, like click url"})
    public Map<String, String> placeholders = new TreeMap<>(Map.ofEntries(
            Map.entry("*check*", "§a✔"),
            Map.entry("*cross*", "§c✖"),
            Map.entry("*star*", "★"),
            Map.entry("*caution*", "⚠"),
            Map.entry("*timer*", "⌛"),
            Map.entry("*clock*", "⌚"),
            Map.entry("*music*", "♫"),
            Map.entry("*peace*", "☮"),
            Map.entry("*hazard*", "☣"),
            Map.entry("*radioactive*", "☢"),
            Map.entry("*snow*", "❄"),
            Map.entry("*pirate*", "☠"),
            Map.entry("<<", "«"),
            Map.entry(">>", "»"),
            Map.entry(":)", "☺"),
            Map.entry(":(", "☹"),
            Map.entry("<3", "§c❤"),
            Map.entry("<inventory>", "<inv>"),
            Map.entry("<i>", "<item>"),
            Map.entry("<enderchest>", "<ec>"),
            Map.entry("discord", "<click:open_url:https://discord.gg/C8d7EqQz>Click to join our discord server</click>"),
            Map.entry("position", "<white><blue>Server:</blue> %server_name% <aqua>World:</aqua> %player_world% <gold>X:</gold> %player_x% <gold>Y:</gold> %player_y% <gold>Z:</gold> %player_z%</white>")
    ));
    @Comment({"Here you can blacklist some terms (like swears, insults and unwanted urls)", "They will be replaced with a *", "You can use the regex syntax and the * wildcard"})
    public List<String> regex_blacklist = List.of(
            "discord.gg/.*",
            "(?i)shit",
            "(?i)sh!t",
            "(?i)niggers?",
            "(?i)fuck",
            "(?i)bicth",
            "(?i)bitch",
            "(?i)dick",
            "(?i)d1ck",
            "(?i)dik",
            "(?i)d1c",
            "(?i)ashole",
            "(?i)azzhole",
            "(?i)nigar",
            "(?i)niger",
            "(?i)c0ck",
            "(?i)kock",
            "(?i)fuck",
            "(?i)cunt",
            "(?i)dickhead",
            "(?i)asshole",
            "(?i)arsehole",
            "(?i)fuckhead",
            "(?i)faggots?",
            "(?i)kkk",
            "(?i)whores?",
            "(?i)sluts?",
            "(?i)cunts?",
            "(?i)dickheads?",
            "(?i)fucktard",
            "(?i)fucker",
            "(?i)pussy",
            "(?i)pussies",
            "(?i)cocks?",
            "(?i)dicks?",
            "(?i)twats?",
            "(?i)hump",
            "(?i)rednecks?",
            "(?i)chingchong",
            "(?i)anus",
            "(?i)bastard",
            "(?i)blowjob",
            "(?i)boner",
            "(?i)boobs?",
            "(?i)boobies",
            "(?i)dildo",
            "(?i)whore",
            "(?i)cum",
            "(?i)heil",
            "(?i)sex",
            "(?i)piss",
            "(?i)raped",
            "(?i)卐",
            "(?i)卍",
            "(?i)♿"
    );
    @Comment({"Links whitelist (regex patterns). Players without permission can still send these links.",
            "Matches against the full URL with or without the scheme (http/https)."})
    public List<String> link_whitelist = List.of();

    @Comment({
            "Announcer configurations",
            "delay is in seconds, how many seconds between each announcement",
            "If you want to disable an announce, just remove it from the list, remember that in yaml [] is an empty list",
            "If you specify a permission, only players with that permission will see the announce. Keep it empty to make it public",
    })
    public List<Announcement> announcer = List.of(new Announcement("default", "<yellow>RedisChat</yellow> <gray>»</gray> <red>To EssentialsX and CMI users: <aqua><br>disable <gold>/msg, /reply, /broadcast, /ignore, etc</gold> commands inside CMI and EssentialsX<br>Or RedisChat commands <red>will <u>not</u> work</red>!!!</aqua>", "public", 300));

    @Comment({"Title of the ShowInventory GUI"})
    public String inv_title = "Inventory of %player%";
    @Comment({"The text inside inventory <tag>"})
    public String inv_tag = "inv";
    @Comment({"Title of the ShowItem GUI"})
    public String item_title = "Item of %player%";
    @Comment({"The text inside item <tag>"})
    public String item_tag = "item";
    @Comment({"Title of the ShowEnderchest GUI"})
    public String ec_title = "Enderchest of %player%";
    @Comment({"The text inside enderchest <tag>"})
    public String ec_tag = "ec";
    public String nothing_tag= "Nothing";
    @Comment({"Title of the ShowShulkerBox GUI"})
    public String shulker_title = "Shulker of %player%";
    @Comment("There are some others chat formats, like broadcast and clear chat messages")
    public String broadcast_format = "<red>Announce <dark_gray>» <white>{message}";
    @Comment({"This message will be sent when a player logs in for the first time",
            "Put an empty string \"\" to disable this feature"})
    public String first_join_message = "<red>Welcome to the server, <white>%player_name%<red>!";
    @Comment("This message will be sent to all players when the chat is cleared")
    public String clear_chat_message = "<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared<br><br><br><br><br><br><br><br><br><br>Chat cleared";

    @Comment("How many messages to keep in the chat history for each player and for each audience")
    public int last_message_keep = 3;
    @Comment("Here you can set the number of messages that a player can send without being rate limited inside public chat")
    public int rate_limit = 3;
    @Comment("Here you can set the time in seconds that a player can send the number of messages specified in rate_limit inside public chat")
    public int rate_limit_time_seconds = 5;
    @Comment("The discord webhook of the public chat")
    public String publicDiscordWebhook = "";
    @Comment("Whether to filter the public chat")
    public boolean isPublicFiltered = true;
    @Comment({
            "Proximity chat settings",
            "Set to -1 to disable proximity chat (normal cross-server chat)",
            "Set to 0 to make it work for the whole local server",
            "Set to a positive number to check if a player is in the same server, same world and below the specified distance",
            "The number is the distance in blocks"
    })
    public int publicProximityDistance = -1;

    @Comment("Rejoin delay in milliseconds")
    public int rejoinSendDelay = 500;
    @Comment("Quit delay in milliseconds")
    public int quitSendWaiting = 3000;
    @Comment({"ALL player string, used in ignore and mute commands to ignore or mute all players",
    "Leave it empty to disable ignoring/muting all players",
    "Don't choose a string that can be a player name!!"})
    public String allPlayersString = "-ALL-";
    @Comment({"Format id:volume:pitch",
            "You can find the list of sounds here: https://jd.papermc.io/paper/1.20/org/bukkit/Sound.html",
            "Leave it empty \"\" to disable the sound"})
    public String mentionSound = "ENTITY_EXPERIENCE_ORB_PICKUP:1:1";
    @Comment("The format is the same as the mention sound, leave empty for no sound")
    public String privateMessageSound = "";
    @Comment("Enable or disable the staff chat")
    public boolean enableStaffChat = true;
    @Comment("Enable or disable the chat color GUI")
    public boolean enableChatColorGUI = false;
    @Comment("Enable to complete chat suggestions with player names from RedisChat's shared player list")
    public boolean completeChatSuggestions = true;
    @Comment("Messages with this prefix will be sent to staff chat")
    public String staffChatPrefix = "!";
    @Comment("The format of the staff chat messages")
    public String staffChatFormat = "<gold>StaffChat </gold> : {message}";
    @Comment("The discord webhook of the staff chat")
    public String staffChatDiscordWebhook = "";
    public String inventoryFormat = "<click:run_command:%command%><gold>[%player%'s Inventory]</gold></click>";
    public String itemFormat = "<click:run_command:%command%>[%amount%%item_name%]</click>";
    @Comment({"Only 1.20.6+", "Whether to use the item name or the display name when parsing the <item> tag"})
    public boolean useItemName = false;
    @Comment({"Whether to use hover on itemshare tags to show item lore"})
    public boolean hoverItem = false;
    public String enderChestFormat = "<click:run_command:%command%><light_purple>[%player%'s EnderChest]</light_purple></click>";

    @Comment("The format of the timestamp in mails (by default is like 31/07/2023 15:24)")
    public String mailTimestampFormat = "dd/MM/yyyy HH:mm";
    @Comment("The timezone of the timestamp in mails (by default is Central European Time)")
    public String mailTimestampZone = "UTC+1";
    @Comment("Resend \"received mail\" message on join if a mail is unread")
    public boolean remindMailOnJoin = true;
    @Comment("Mail line char size")
    public int mailLineSize = 19;
    @Comment("Those commands will be disabled")
    public List<String> disabledCommands = List.of();
    @Comment("The [inv], [item] and [ec] placeholders will be considered as minimessage tags")
    public boolean interactiveChatNostalgia = true;
    @Comment("Command aliases (works for msg, mail, reply, staffchat and channel)")
    public Map<String, List<String>> commandAliases = new HashMap<>(Map.of(
            "msg", List.of("rmsg", "whisper", "tell", "w"),
            "rmail", List.of("mail", "mails"),
            "reply", List.of("r"),
            "channel", List.of("ch", "channels"),
            "staffchat", List.of("sc"),
            "rmutechat", List.of("mutechat", "mute"),
            "runmutechat", List.of("unmutechat", "unmute"),
            "rbroadcast", List.of("bc", "broadcast"),
            "rbroadcastraw", List.of("bcraw", "broadcastraw"),
            "announcer", List.of("announce")
    ));

    @Comment("Re-enables bukkit color glitches for colored placeholders")
    public boolean enablePlaceholderGlitch = true;
    @Comment("Register tag integrations (Like Oraxen Integration which is internal)")
    public boolean useTagsIntegration = false;
    @Comment({"The priority of the listening event (LOWEST, LOW, NORMAL, HIGH, HIGHEST, MONITOR)",
            "adjust this if other plugins are interfering with RedisChat"})
    public String listeningPriority = "NORMAL";
    @Comment("Toggle debug mode (by default is false)")
    public boolean debug = false;
    @Comment("Toggle debug mode for playerlist (by default is false)")
    public boolean debugPlayerList = false;
    @Comment("Toggle debug mode for itemshare (by default is false)")
    public boolean debugItemShare = false;
    @Comment("The number of threads for chat tasks")
    public int chatThreads = 2;
    @Comment({"botName is the botId associated to the bot inside the spicord configuration",
            "Every channel of RedisChat is linked with a channel on Discord",
            "The first element is a RedisChat channel, the second one is a Discord channel id",
            "You can find the Discord channel id by right clicking on the channel and clicking on 'Copy ID'"})
    public SpicordSettings spicord = new SpicordSettings(true, true, "<blue>[Discord]</blue> %role% %username% » {message}", "**%channel%** %sender% » {message}", Map.of("public", "1127207189547847740"));
    @Comment({
            "World channels (disabled by default).",
            "When enabled, players will be switched to the channel bound to their current world.",
            "Messages sent in a world-bound channel are only delivered to players in that world.",
            "If autoCreate is enabled, missing channels will be created using the template.",
            "autoCreatePrefixes supports prefix strings (you can also use a trailing '*' like 'raid_*').",
            "localOnly makes auto-created channels local to this server only (not stored in Redis).",
            "usePlayerFormats wraps the template format around the per-permission player format.",
            "useWorldFormats wraps the template format around the per-permission world format list.",
            "When both usePlayerFormats and useWorldFormats are true, world formats take precedence.",
            "world formats only define: permission + format."
    })
    public WorldChannelSettings worldChannels = new WorldChannelSettings(
            false,
            false,
            false,
            false,
            false,
            List.of(),
            Map.of(),
            new WorldChannelTemplate(
                    "§6[%world%]",
                    "<gray>[%world%]</gray>%luckperms_prefix%%player <dark_gray>>></dark_gray> <gray>{message}</gray>",
                    5,
                    3,
                    true,
                    false,
                    false,
                    ""
            ),
            List.of(),
            new WorldChatFormat("none", defaultFormat.format())
    );

    @Override
    public boolean validateConfig() {
        boolean modified = false;
        if (worldChannels == null) {
            worldChannels = new WorldChannelSettings(
                    false,
                    false,
                    false,
                    false,
                    false,
                    List.of(),
                    Map.of(),
                    new WorldChannelTemplate(
                            "<gold>[%world%]</gold>",
                            "<gray>[%world%]</gray> {message}",
                            5,
                            3,
                            true,
                            true,
                            false,
                            ""
                    ),
                    List.of(),
                    new WorldChatFormat("none", defaultFormat.format())
            );
            modified = true;
        } else {
            boolean worldChannelsChanged = false;
            boolean localOnly = worldChannels.localOnly();
            boolean usePlayerFormats = worldChannels.usePlayerFormats();
            boolean useWorldFormats = worldChannels.useWorldFormats();
            List<String> prefixes = worldChannels.autoCreatePrefixes();
            if (prefixes == null) {
                prefixes = List.of();
                worldChannelsChanged = true;
            }
            Map<String, String> bindings = worldChannels.bindings();
            if (bindings == null) {
                bindings = Map.of();
                worldChannelsChanged = true;
            }
            WorldChannelTemplate template = worldChannels.template();
            if (template == null) {
                template = new WorldChannelTemplate(
                        "<gold>[%world%]</gold>",
                        "<gray>[%world%]</gray> {message}",
                        5,
                        3,
                        true,
                        true,
                        false,
                        ""
                );
                worldChannelsChanged = true;
            }

            List<WorldChatFormat> worldFormats = worldChannels.formats();
            if (worldFormats == null) {
                worldFormats = List.of();
                worldChannelsChanged = true;
            }

            WorldChatFormat worldDefaultFormat = worldChannels.defaultFormat();
            if (worldDefaultFormat == null) {
                worldDefaultFormat = new WorldChatFormat("none", defaultFormat.format());
                worldChannelsChanged = true;
            } else {
                String permission = worldDefaultFormat.permission() != null ? worldDefaultFormat.permission() : "none";
                String messageFormat = worldDefaultFormat.format() != null ? worldDefaultFormat.format() : defaultFormat.format();
                if (!Objects.equals(permission, worldDefaultFormat.permission())
                        || !Objects.equals(messageFormat, worldDefaultFormat.format())) {
                    worldDefaultFormat = new WorldChatFormat(permission, messageFormat);
                    worldChannelsChanged = true;
                }
            }

            boolean worldFormatsChanged = false;
            List<WorldChatFormat> normalizedWorldFormats = new ArrayList<>(worldFormats.size());
            for (WorldChatFormat format : worldFormats) {
                if (format == null) {
                    normalizedWorldFormats.add(worldDefaultFormat);
                    worldFormatsChanged = true;
                    continue;
                }
                String permission = format.permission() != null ? format.permission() : worldDefaultFormat.permission();
                String messageFormat = format.format() != null ? format.format() : worldDefaultFormat.format();
                if (!Objects.equals(permission, format.permission())
                        || !Objects.equals(messageFormat, format.format())) {
                    worldFormatsChanged = true;
                }

                normalizedWorldFormats.add(new WorldChatFormat(permission, messageFormat));
            }

            if (worldFormatsChanged) {
                worldFormats = List.copyOf(normalizedWorldFormats);
                worldChannelsChanged = true;
            }

            if (worldChannelsChanged) {
                worldChannels = new WorldChannelSettings(
                        worldChannels.enabled(),
                        worldChannels.autoCreate(),
                        localOnly,
                        usePlayerFormats,
                        useWorldFormats,
                        prefixes,
                        bindings,
                        template,
                        worldFormats,
                        worldDefaultFormat
                );
                modified = true;
            }
        }
        if (formats == null) {
            formats = List.of();
            modified = true;
        }

        boolean formatsChanged = false;
        List<ChatFormat> normalizedFormats = new ArrayList<>(formats.size());
        for (ChatFormat format : formats) {
            if (format == null) {
                normalizedFormats.add(defaultFormat);
                formatsChanged = true;
                continue;
            }
            String permission = format.permission() != null ? format.permission() : defaultFormat.permission();
            String messageFormat = format.format() != null ? format.format() : defaultFormat.format();
            String privateFormat = format.private_format() != null ? format.private_format() : defaultFormat.private_format();
            String receivePrivateFormat = format.receive_private_format() != null ? format.receive_private_format() : defaultFormat.receive_private_format();
            String mentionFormat = format.mention_format() != null ? format.mention_format() : defaultFormat.mention_format();
            String linkFormat = format.link_format() != null ? format.link_format() : defaultFormat.link_format();
            String joinFormat = format.join_format() != null ? format.join_format() : defaultFormat.join_format();
            String quitFormat = format.quit_format() != null ? format.quit_format() : defaultFormat.quit_format();

            if (!Objects.equals(permission, format.permission())
                    || !Objects.equals(messageFormat, format.format())
                    || !Objects.equals(privateFormat, format.private_format())
                    || !Objects.equals(receivePrivateFormat, format.receive_private_format())
                    || !Objects.equals(mentionFormat, format.mention_format())
                    || !Objects.equals(linkFormat, format.link_format())
                    || !Objects.equals(joinFormat, format.join_format())
                    || !Objects.equals(quitFormat, format.quit_format())) {
                formatsChanged = true;
            }

            normalizedFormats.add(new ChatFormat(permission, messageFormat, privateFormat, receivePrivateFormat,
                    mentionFormat, linkFormat, joinFormat, quitFormat));
        }

        if (formatsChanged) {
            formats = List.copyOf(normalizedFormats);
            modified = true;
        }

        formats.forEach(format -> {
            if (!format.format().contains("{message}")) {
                Bukkit.getLogger().severe("Format " + format.permission() + " doesn't contain {message} placeholder");
            }
            if (!format.private_format().contains("{message}")) {
                Bukkit.getLogger().severe("Private format " + format.permission() + " doesn't contain {message} placeholder");
            }
            if (!format.receive_private_format().contains("{message}")) {
                Bukkit.getLogger().severe("Receive private format " + format.permission() + " doesn't contain {message} placeholder");
            }
            if (format.quit_format() == null) {
                Bukkit.getLogger().severe("Quit format " + format.permission() + " is empty. TO DISABLE IT, SET IT TO \"\"");
            }
            if (format.join_format() == null) {
                Bukkit.getLogger().severe("Join format " + format.permission() + " is empty. TO DISABLE IT, SET IT TO \"\"");
            }
            if (!dataMedium.equals(DataType.REDIS.keyName)) {
                Bukkit.getLogger().warning("You're not using REDIS as data medium, it is recommended to use it or you may not be able to use some features");
            }
        });
        if (!defaultFormat.format().contains("{message}")) {
            Bukkit.getLogger().warning("Default format doesn't contain {message} placeholder");
        }
        if (!defaultFormat.private_format().contains("{message}")) {
            Bukkit.getLogger().warning("Default private format doesn't contain {message} placeholder");
        }
        if (!defaultFormat.receive_private_format().contains("{message}")) {
            Bukkit.getLogger().warning("Default receive private format doesn't contain {message} placeholder");
        }
        if (!defaultFormat.mention_format().contains("%player%")) {
            Bukkit.getLogger().warning("Default mention format doesn't contain %player% placeholder");
        }
        if (!commandAliases.containsKey("mutechat")) {
            commandAliases = new HashMap<>(commandAliases);
            commandAliases.put("mutechat", List.of("mute"));
            modified = true;
            Bukkit.getLogger().warning("You didn't set any aliases for mutechat, using default aliases");
        }
        if (!commandAliases.containsKey("unmutechat")) {
            commandAliases = new HashMap<>(commandAliases);
            commandAliases.put("unmutechat", List.of("unmute"));
            modified = true;
            Bukkit.getLogger().warning("You didn't set any aliases for unmutechat, using default aliases");
        }
        if (!commandAliases.containsKey("rbroadcast")) {
            commandAliases = new HashMap<>(commandAliases);
            commandAliases.put("rbroadcast", List.of("broadcast", "bc"));
            modified = true;
            Bukkit.getLogger().warning("You didn't set any aliases for rbroadcast, using default aliases");
        }
        if (!commandAliases.containsKey("rbroadcastraw")) {
            commandAliases = new HashMap<>(commandAliases);
            commandAliases.put("rbroadcastraw", List.of("broadcastraw", "bcraw"));
            modified = true;
            Bukkit.getLogger().warning("You didn't set any aliases for rbroadcastraw, using default aliases");
        }
        if (dataMedium.equalsIgnoreCase("H2+PM")) {
            dataMedium = DataType.SQLITE.keyName;
            modified = true;
            Bukkit.getLogger().warning("H2+PM has been deprecated, using SQLITE+PM as default");
        }
        for (Announcement announcement : announcer) {
            if (announcement.channelName == null || announcement.channelName.isEmpty()) {
                Bukkit.getLogger().warning("Announce " + announcement.announcementName() + " doesn't have a channel name, using \"public\" as default");
            }
        }
        if (!privateMessageSound.isEmpty() && privateMessageSound.split(":").length != 3) {
            Bukkit.getLogger().warning("Private message sound format is invalid!!!, using no sound");
            privateMessageSound = "";
        }
        if (!mentionSound.isEmpty() && mentionSound.split(":").length != 3) {
            Bukkit.getLogger().warning("Mention sound format is invalid!!!, using no sound");
            mentionSound = "";
        }

        if (redis.poolSize <= 1 && dataMedium.equals(DataType.REDIS.keyName)) {
            Bukkit.getLogger().warning("Redis pool size is too low, setting it to 2");
            redis = new RedisSettings(redis.host, redis.port, redis.user, redis.password, redis.database, redis.timeout, redis.clientName, 2);
            modified = true;
        }

        return modified;
    }

    public record RedisSettings(String host, int port, String user, String password,
                                int database, int timeout,
                                String clientName, int poolSize) {
    }

    public record Mysql(
            String host,
            int port,
            String database,
            String driverClass,
            String connectionParameters,
            String username,
            String password,
            int poolSize,
            int poolIdle,
            long poolLifetime,
            long poolKeepAlive,
            long poolTimeout) {
    }


    public record Announcement(
            String announcementName,
            String message,
            String channelName,
            int delay) {
    }

    public record SpicordSettings(
            boolean enabled,
            boolean discordReceiver,
            String chatFormat,
            String discordFormat,
            Map<String, String> spicordChannelLink
    ) {
    }

    public record WorldChannelSettings(
            boolean enabled,
            boolean autoCreate,
            boolean localOnly,
            boolean usePlayerFormats,
            boolean useWorldFormats,
            List<String> autoCreatePrefixes,
            Map<String, String> bindings,
            WorldChannelTemplate template,
            List<WorldChatFormat> formats,
            WorldChatFormat defaultFormat
    ) {
    }

    public record WorldChannelTemplate(
            String displayName,
            String format,
            int rateLimit,
            int rateLimitPeriod,
            boolean filtered,
            boolean shownByDefault,
            boolean permissionEnabled,
            String notificationSound
    ) {
    }

    public record WorldChatFormat(
            @NotNull String permission,
            @NotNull String format
    ) {
    }

    public @NotNull ChatFormat getChatFormat(@Nullable CommandSender p) {
        if (p == null) return defaultFormat;
        return formats.stream()
                .filter(format -> p.hasPermission(format.permission()))
                .findFirst()
                .orElse(defaultFormat);
    }

    public @NotNull String getWorldChatMessageFormat(@Nullable CommandSender p) {
        String fallback = defaultFormat.format();
        if (worldChannels != null && worldChannels.defaultFormat() != null) {
            fallback = worldChannels.defaultFormat().format();
        }
        if (p == null) return fallback;
        if (worldChannels == null || worldChannels.formats() == null || worldChannels.formats().isEmpty()) {
            return fallback;
        }
        return worldChannels.formats().stream()
                .filter(format -> p.hasPermission(format.permission()))
                .findFirst()
                .map(WorldChatFormat::format)
                .orElse(fallback);
    }

    public String[] getCommandAliases(String command) {
        return commandAliases.getOrDefault(command, List.of()).toArray(new String[0]);
    }

    public DataType getDataType() {
        final DataType type = DataType.fromString(dataMedium.toUpperCase());
        return type == null ? DataType.SQLITE : type;
    }

    public enum DataType {
        MYSQL("MYSQL+PM"),
        REDIS("REDIS"),
        SQLITE("SQLITE+PM"),
        ;
        private final String keyName;

        /**
         * @param keyName the name of the key
         */
        DataType(final String keyName) {
            this.keyName = keyName;
        }

        public static DataType fromString(String text) {
            for (DataType b : DataType.values()) {
                if (b.keyName.equalsIgnoreCase(text)) {
                    return b;
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return keyName;
        }
    }
}
