package net.officefloor.hq.app.patron;

/** Body of naming a patron for a person: who is doing it, and the id of the person named. */
public record PatronRequest(String user, Long patronId) {
}
