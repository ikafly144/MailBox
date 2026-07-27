package net.sabafly.mailBox.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * i18n locale manager.
 * Loads locale overrides from plugins/MailBox/locales/{locale}.yml
 * and applies them to a Messages instance.
 *
 * Built-in locales: en (default), zh_CN, ja_JP
 */
public class LocaleManager {

    private static final Logger logger = LoggerFactory.getLogger(LocaleManager.class);

    private final Path dataDir;
    private Locale currentLocale = Locale.EN;
    private Messages messages = new Messages();

    public LocaleManager(Path dataDir) {
        this.dataDir = dataDir;
    }

    public Locale currentLocale() {
        return currentLocale;
    }

    /**
     * @return the current Messages instance with locale overrides applied
     */
    public Messages messages() {
        return messages;
    }

    /**
     * Load locale file and apply overrides to Messages.
     *
     * @param locale the locale to load
     */
    public void loadLocale(Locale locale) {
        this.currentLocale = locale;
        this.messages = new Messages();

        ensureLocaleFilesExist();

        Path localeFile = dataDir.resolve("locales").resolve(locale.fileName() + ".yml");
        if (!Files.exists(localeFile)) {
            logger.warn("Locale file not found: {}, falling back to EN", localeFile);
            this.currentLocale = Locale.EN;
            localeFile = dataDir.resolve("locales").resolve(Locale.EN.fileName() + ".yml");
            if (!Files.exists(localeFile)) {
                logger.error("Default locale file en.yml not found, using built-in defaults");
                return;
            }
        }

        try {
            YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                    .path(localeFile)
                    .build();
            var node = loader.load();
            Map<String, String> localeMap = flattenNode(node, "");
            applyToMessages(messages, localeMap);
            logger.info("Loaded locale: {}", currentLocale.fileName());
        } catch (Exception e) {
            logger.error("Failed to load locale: {}", currentLocale.fileName(), e);
            this.currentLocale = Locale.EN;
        }
    }

    /**
     * Flatten a Configurate node into a flat key-value map.
     * e.g. "read" -> "<green>Read</green>"
     */
    private Map<String, String> flattenNode(org.spongepowered.configurate.ConfigurationNode node, String prefix) {
        Map<String, String> result = new HashMap<>();
        if (node.virtual()) return result;

        for (var entry : node.childrenMap().entrySet()) {
            String key = prefix.isEmpty()
                    ? entry.getKey().toString()
                    : prefix + "." + entry.getKey().toString();
            var child = entry.getValue();

            if (child.isMap()) {
                result.putAll(flattenNode(child, key));
            } else {
                String value = child.getString();
                if (value != null) {
                    result.put(key, value);
                }
            }
        }
        return result;
    }

    /**
     * Apply locale values to Messages fields via reflection.
     * Locale keys use kebab-case (e.g. "read", "new-mail", "click-action-delete").
     * Field names in Messages use camelCase (e.g. "read", "newMail", "clickActionDelete").
     */
    private void applyToMessages(Messages messages, Map<String, String> localeMap) {
        for (var entry : localeMap.entrySet()) {
            String localeKey = entry.getKey();
            String value = entry.getValue();
            String fieldName = kebabToCamel(localeKey);

            try {
                Field field = Messages.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                Object current = field.get(messages);
                if (current instanceof String) {
                    field.set(messages, value);
                }
            } catch (NoSuchFieldException e) {
                logger.debug("Unknown locale key: {}", localeKey);
            } catch (Exception e) {
                logger.warn("Failed to apply locale key {}: {}", localeKey, e.getMessage());
            }
        }
    }

    /**
     * Convert kebab-case to camelCase.
     * "click-action-delete" -> "clickActionDelete"
     * "read" -> "read"
     */
    private String kebabToCamel(String kebab) {
        String[] parts = kebab.split("-");
        if (parts.length == 1) return parts[0];

        StringBuilder sb = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                sb.append(Character.toUpperCase(parts[i].charAt(0)));
                if (parts[i].length() > 1) {
                    sb.append(parts[i].substring(1));
                }
            }
        }
        return sb.toString();
    }

    /**
     * Ensure the locales directory and built-in locale files exist.
     */
    private void ensureLocaleFilesExist() {
        Path localesDir = dataDir.resolve("locales");
        try {
            Files.createDirectories(localesDir);
        } catch (IOException e) {
            logger.error("Failed to create locales directory", e);
            return;
        }

        for (Locale locale : Locale.values()) {
            copyResourceIfMissing("/locales/" + locale.fileName() + ".yml", localesDir.resolve(locale.fileName() + ".yml"));
        }
    }

    private void copyResourceIfMissing(String resource, Path target) {
        if (Files.exists(target)) return;
        try (InputStream in = getClass().getResourceAsStream(resource)) {
            if (in == null) {
                logger.warn("Bundled locale resource not found: {}", resource);
                return;
            }
            Files.copy(in, target);
        } catch (IOException e) {
            logger.error("Failed to copy locale resource: {}", resource, e);
        }
    }
}
