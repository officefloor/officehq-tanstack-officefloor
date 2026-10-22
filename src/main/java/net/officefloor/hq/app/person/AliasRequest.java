package net.officefloor.hq.app.person;

/** Body of giving a person an alias: who is doing it, and the alias. */
public record AliasRequest(String user, String alias) {
}
