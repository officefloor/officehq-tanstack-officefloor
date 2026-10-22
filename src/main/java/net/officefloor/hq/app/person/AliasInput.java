package net.officefloor.hq.app.person;

/** A validated alias: the signed-in user, and the alias trimmed and not blank. */
public record AliasInput(String user, String alias) {
}
