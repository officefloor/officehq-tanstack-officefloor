package net.officefloor.hq.app.person;

/** Body of an alias, as typed: the alias and who adds it. */
public record AliasRequest(String alias, String user) {
}
