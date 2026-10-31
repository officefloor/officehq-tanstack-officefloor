package net.officefloor.hq.app.person;

/** Body of adding an alias: the alias, and who makes the change. */
public record AliasRequest(String alias, String user) {
}
