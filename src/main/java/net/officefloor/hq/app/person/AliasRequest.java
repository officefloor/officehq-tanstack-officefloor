package net.officefloor.hq.app.person;

/** Body of giving a person an alias: the alias, and who gives it. */
public record AliasRequest(String alias, String by) {
}
