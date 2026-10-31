package net.officefloor.hq.app.person;

/** Body of adding a patron: the person named as patron, and who makes the change. */
public record PatronRequest(Long patronId, String user) {
}
