package net.officefloor.hq.app.alias;

/** Body of POST /api/sheets/{sheet}/people/{person}/aliases: the alias to add. */
public record AliasRequest(String alias) {
}
