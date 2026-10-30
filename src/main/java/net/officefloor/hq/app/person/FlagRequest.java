package net.officefloor.hq.app.person;

/** Body of POST /api/sheets/{sheetId}/people/{personId}/flag: the flag's reason, its until date and who flags. */
public record FlagRequest(String reason, String until, String user) {
}
