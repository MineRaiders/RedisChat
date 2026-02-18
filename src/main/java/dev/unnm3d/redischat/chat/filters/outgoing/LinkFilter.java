package dev.unnm3d.redischat.chat.filters.outgoing;

import dev.unnm3d.redischat.Permissions;
import dev.unnm3d.redischat.RedisChat;
import dev.unnm3d.redischat.api.objects.ChatMessage;
import dev.unnm3d.redischat.chat.filters.AbstractFilter;
import dev.unnm3d.redischat.chat.filters.FilterResult;
import dev.unnm3d.redischat.settings.FiltersConfig;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinkFilter extends AbstractFilter<FiltersConfig.FilterSettings> {
    private static final Pattern URL_PATTERN = Pattern.compile("((?:https?://)?[a-zA-Z0-9.]+\\.[a-zA-Z0-9]{2,32})([a-zA-Z0-9/?.%=\\-_&]*)");
    private final RedisChat plugin;

    public LinkFilter(RedisChat plugin, FiltersConfig.FilterSettings filterSettings) {
        super("link", Direction.OUTGOING, filterSettings);
        this.plugin = plugin;
    }

    @Override
    public FilterResult applyWithPrevious(CommandSender sender, @NotNull ChatMessage message, ChatMessage... previousMessages) {
        if (sender.hasPermission(Permissions.SEND_LINKS.getPermission())) {
            return new FilterResult(message, false);
        }

        final Matcher matcher = URL_PATTERN.matcher(message.getContent());
        if (!matcher.find()) {
            return new FilterResult(message, false);
        }

        final Set<String> blockedLinks = new LinkedHashSet<>();
        matcher.reset();
        while (matcher.find()) {
            final String domainString = matcher.group(1);
            final String pathString = matcher.group(2);
            final String rawLink = domainString + pathString;
            final String normalizedLink = domainString.startsWith("http")
                    ? rawLink
                    : "https://" + rawLink;

            if (!isWhitelisted(rawLink, normalizedLink)) {
                blockedLinks.add(rawLink);
            }
        }

        if (!blockedLinks.isEmpty()) {
            return new FilterResult(message, true, Optional.of(
                    plugin.getComponentProvider().parseWithSafeReplacements(sender,
                            plugin.messages.linkNotAllowed,
                            Map.of("%links%", String.join(", ", blockedLinks)))
            ));
        }

        return new FilterResult(message, false);
    }

    private boolean isWhitelisted(String rawLink, String normalizedLink) {
        if (plugin.config.link_whitelist == null || plugin.config.link_whitelist.isEmpty()) {
            return false;
        }
        for (String entry : plugin.config.link_whitelist) {
            final Pattern whitelistPattern = Pattern.compile(entry, Pattern.CASE_INSENSITIVE);
            if (whitelistPattern.matcher(rawLink).find() || whitelistPattern.matcher(normalizedLink).find()) {
                return true;
            }
        }
        return false;
    }
}
