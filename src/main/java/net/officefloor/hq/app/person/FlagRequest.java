package net.officefloor.hq.app.person;

/** A flag to set on a person: its reason, the date it holds until, and the signed-in user (`by`) setting it. */
public record FlagRequest(String reason, String until, String by) {
}
