package net.officefloor.hq.app.setting;

/**
 * Settings as the screen sends them, each only when it is being changed (null: left as it is): the
 * clerk limit, with up to two decimals (blank: no limit), and the floor, with up to two decimals.
 */
public record SettingsRequest(String clerkLimit, String floor) {
}
