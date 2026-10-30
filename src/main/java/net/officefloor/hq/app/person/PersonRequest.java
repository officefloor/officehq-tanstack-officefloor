package net.officefloor.hq.app.person;

/** Body of POST /api/sheets/{sheetId}/people and PUT /api/sheets/{sheetId}/people/{personId}.
 *  {@code version} (PUT only) is the version of the person the editor loaded. */
public record PersonRequest(String name, String grade, String user, Integer version) {
}
