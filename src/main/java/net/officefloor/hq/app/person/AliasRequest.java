package net.officefloor.hq.app.person;

/** Body of POST /api/sheets/{sheetId}/people/{personId}/aliases: the alias, and who adds it. */
public record AliasRequest(String alias, String user) {
}
