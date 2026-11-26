package dev.yukio.regionaccess.region;

public enum ViolationType {
    /** Регион закрыт белым списком, игрок не участник. */
    WHITELIST("whitelist-warning", "punished-whitelist"),
    /** Игрок внесён в чёрный список региона. */
    BLACKLIST("blacklist-warning", "punished-blacklist");

    private final String warningKey;
    private final String punishedKey;

    ViolationType(String warningKey, String punishedKey) {
        this.warningKey = warningKey;
        this.punishedKey = punishedKey;
    }

    public String warningKey() {
        return warningKey;
    }

    public String punishedKey() {
        return punishedKey;
    }
}
