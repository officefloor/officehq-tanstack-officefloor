package net.officefloor.hq.app.setting;

/** The settings form as submitted: the clerk limit (blank: no limit), the floor (blank: 1.00) and the terms in
 *  days (blank: 14) and the late percent
 *  (blank: 0.00); a key left out (null) is not changed. */
public record SettingsRequest(String clerkLimit, String floor, String terms, String latePct) {
}
