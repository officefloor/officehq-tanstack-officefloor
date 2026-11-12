package net.officefloor.hq.app.claim;

/** Body of a claim's move to another person, as sent: the id of the person to move it to and who moves it. */
public record ReassignRequest(String to, String user) {
}
