package net.officefloor.hq.app.person;

/** Body of POST /api/sheets/{sheetId}/people/{personId}/move: whether the person moves up (true) or down
 *  (false) the list, swapping places with their neighbour that way. */
public record MoveRequest(boolean up, String user) {
}
