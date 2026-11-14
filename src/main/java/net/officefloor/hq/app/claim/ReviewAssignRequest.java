package net.officefloor.hq.app.claim;

/** What assigning a review submits: the user to assign it to (null or blank: none typed). */
public record ReviewAssignRequest(String user) {
}
