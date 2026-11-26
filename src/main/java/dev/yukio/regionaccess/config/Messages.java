package dev.yukio.regionaccess.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Сообщения из раздела messages файла config.yml.
 * Пустая строка в конфиге означает «не отправлять».
 */
public final class Messages {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final FileConfiguration cfg;
    private final TagResolver prefix;

    public Messages(FileConfiguration cfg) {
        this.cfg = cfg;
        this.prefix = Placeholder.parsed("prefix", cfg.getString("messages.prefix", ""));
    }

    /** Сырой текст сообщения; null, если сообщение отключено (пустая строка). */
    public String raw(String key) {
        String value = cfg.getString("messages." + key);
        if (value == null) {
            return "<red>[RegionAccess] Не найдено сообщение messages." + key;
        }
        return value.isBlank() ? null : value;
    }

    /** Готовый компонент; null, если сообщение отключено. */
    public Component get(String key, TagResolver... resolvers) {
        String raw = raw(key);
        if (raw == null) {
            return null;
        }
        return MM.deserialize(raw, TagResolver.resolver(prefix, TagResolver.resolver(resolvers)));
    }

    /** Как {@link #get}, но вместо null возвращает пустой компонент. */
    public Component getOrEmpty(String key, TagResolver... resolvers) {
        Component c = get(key, resolvers);
        return c == null ? Component.empty() : c;
    }

    public void send(CommandSender to, String key, TagResolver... resolvers) {
        Component c = get(key, resolvers);
        if (c != null) {
            to.sendMessage(c);
        }
    }

    public static TagResolver region(String id) {
        return Placeholder.unparsed("region", id);
    }

    public static TagResolver player(String name) {
        return Placeholder.unparsed("player", name);
    }

    public static TagResolver number(String key, int value) {
        return Placeholder.unparsed(key, String.valueOf(value));
    }
}
