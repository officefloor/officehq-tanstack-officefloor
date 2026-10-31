package net.officefloor.hq.app.claim;

/** Body of a claim's reassignment: the id of the person on the sheet it moves to, and who moves it. */
public record ClaimReassignRequest(Long toPersonId, String user) {
}
