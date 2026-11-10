package net.officefloor.hq.app.delegation;

/** A delegation as the screen sends it: a user name, a from-date and a to-date (yyyy-MM-dd; blank from: today). */
public record DelegationRequest(String user, String from, String to) {
}
