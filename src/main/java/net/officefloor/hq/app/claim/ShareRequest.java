package net.officefloor.hq.app.claim;

/** A share of a claim as the screen sends it: the person to share it with (their id) and their share percent. */
public record ShareRequest(String personId, String share) {
}
