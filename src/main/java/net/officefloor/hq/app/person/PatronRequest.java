package net.officefloor.hq.app.person;

/** A patron as the screen sends it: the id of the person named as patron. */
public record PatronRequest(String patronId) {
}
