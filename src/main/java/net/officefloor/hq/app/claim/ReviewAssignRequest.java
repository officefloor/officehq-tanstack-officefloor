package net.officefloor.hq.app.claim;

/** Body of assigning a REVIEW claim: the user it is assigned to, and who assigns it. */
public record ReviewAssignRequest(String user, String by) {
}
