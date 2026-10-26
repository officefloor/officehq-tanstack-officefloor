package net.officefloor.hq.app.setting;

/** The body of the settings being saved, as typed: the clerk limit (blank: no limit) and the floor, the terms and the late percent (each null or blank: left as it is). */
public record SettingsRequest(String clerkLimit, String floor, String terms, String latePct) {
}
