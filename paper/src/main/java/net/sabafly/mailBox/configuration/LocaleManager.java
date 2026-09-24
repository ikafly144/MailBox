package net.sabafly.mailBox.configuration;

import net.sabafly.mailBox.configuration.type.DurationOrDisabledSerializer;
import net.sabafly.mailBox.configuration.type.DurationSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.util.MapFactories;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

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
    public void loadLocale(Locale locale) throws ConfigurateException {
        ensureLocaleFilesExist();
        Path localeFile = dataDir.resolve("locales").resolve(locale.fileName() + ".yml");

        YamlConfigurationLoader loader = YamlConfigurationLoader.builder().path(localeFile).nodeStyle(NodeStyle.BLOCK).indent(2).defaultOptions(options -> options.shouldCopyDefaults(true).mapFactory(MapFactories.insertionOrdered()).serializers(builder -> builder.register(DurationSerializer.SERIALIZER).register(DurationOrDisabledSerializer.SERIALIZER).build())).build();

        this.currentLocale = locale;
        this.messages = loader.load().get(Messages.class, new Messages());
        logger.info("Loaded locale: {}", currentLocale.fileName());
    }

    /**
     * Save the given Messages instance to the specified locale file.
     *
     * @param messages the Messages instance to save
     * @param locale the locale to save the messages for
     */
    public void saveLocale(Messages messages, Locale locale) {
        ensureLocaleFilesExist();
        Path localeFile = dataDir.resolve("locales").resolve(locale.fileName() + ".yml");

        YamlConfigurationLoader loader = YamlConfigurationLoader.builder().path(localeFile).nodeStyle(NodeStyle.BLOCK).indent(2).defaultOptions(options -> options.shouldCopyDefaults(true).mapFactory(MapFactories.insertionOrdered()).serializers(builder -> builder.register(DurationSerializer.SERIALIZER).register(DurationOrDisabledSerializer.SERIALIZER).build())).build();
        try {
            loader.save(loader.createNode().set(Messages.class, messages));
            logger.info("Saved locale: {}", locale.fileName());
        } catch (ConfigurateException e) {
            logger.error("Failed to save locale: {}", locale.fileName(), e);
        }
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
