package net.sabafly.mailBox.configuration;

/**
 * Supported locales for MailBox i18n.
 * Each locale corresponds to a bundled resource file under /locales/{fileName}.yml.
 */
public enum Locale {

    EN("en"),
    ZH_CN("zh_CN"),
    JA_JP("ja_JP");

    private final String fileName;

    Locale(String fileName) {
        this.fileName = fileName;
    }

    /**
     * @return the locale file name without extension (e.g. "en", "zh_CN")
     */
    public String fileName() {
        return fileName;
    }

    /**
     * Resolve a locale string to an enum value, falling back to EN.
     *
     * @param locale the locale string (e.g. "en", "zh_CN", "ja_JP")
     * @return the matching Locale, or EN if not found
     */
    public static Locale fromString(String locale) {
        if (locale == null || locale.isBlank()) return EN;
        for (Locale l : values()) {
            if (l.fileName.equalsIgnoreCase(locale) || l.name().equalsIgnoreCase(locale.replace("-", "_"))) {
                return l;
            }
        }
        return EN;
    }
}
