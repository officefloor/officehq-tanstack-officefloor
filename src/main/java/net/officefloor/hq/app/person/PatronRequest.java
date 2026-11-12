package net.officefloor.hq.app.person;

/** Body of a patron added: who adds it, and the id of the person named as patron. */
public record PatronRequest(String user, Long patronId) {
}
