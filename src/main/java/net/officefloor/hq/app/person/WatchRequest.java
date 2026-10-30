package net.officefloor.hq.app.person;

/** Body of POST /api/sheets/{sheetId}/people/{personId}/watch: whether the user starts (true) or stops
 *  (false) watching the person. */
public record WatchRequest(boolean on, String user) {
}
