package net.officefloor.hq.app.person;

/** Body of POST /api/sheets/{sheetId}/import-people: the pasted lines and the user adding them. */
public record ImportPeopleRequest(String text, String user) {
}
