package dev.yukio.regionaccess.region;

/** Игрок находится там, где ему быть нельзя. */
public record Violation(ViolationType type, String world, String regionId) {

    public String key() {
        return type + ":" + world + ":" + regionId;
    }
}
