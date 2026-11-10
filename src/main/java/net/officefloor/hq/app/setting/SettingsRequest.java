package net.officefloor.hq.app.setting;

/** Settings as the screen sends them: the clerk limit, with up to two decimals (blank: no limit). */
public record SettingsRequest(String clerkLimit) {
}
