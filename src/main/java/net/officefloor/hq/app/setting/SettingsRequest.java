package net.officefloor.hq.app.setting;

/** The settings form as submitted: the clerk limit (blank: no limit) and the floor (blank: 1.00); a key left out
 *  (null) is not changed. */
public record SettingsRequest(String clerkLimit, String floor) {
}
