package net.officefloor.hq.app.setting;

/** Body of a settings save: who is making it and the settings it changes — the clerk limit (blank: no limit) and
 *  the floor; one left out (null) is left as it is. */
public record SettingsRequest(String user, String clerkLimit, String floor) {
}
