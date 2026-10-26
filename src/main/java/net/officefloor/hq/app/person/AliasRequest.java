package net.officefloor.hq.app.person;

/** The alias to give a person, and the signed-in user (`by`) giving it. */
public record AliasRequest(String alias, String by) {
}
