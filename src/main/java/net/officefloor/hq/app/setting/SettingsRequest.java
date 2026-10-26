package net.officefloor.hq.app.setting;

/** The body of the settings being saved, as typed: the clerk limit (blank: no limit) and the floor and the terms (each null or blank: left as it is). */
public record SettingsRequest(String clerkLimit, String floor, String terms) {
}
