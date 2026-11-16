package net.officefloor.hq.app.patron;

/** Body of adding a patron: the id of the person named as patron, and who does it. */
public record PatronRequest(Long patronId, String by) {
}
