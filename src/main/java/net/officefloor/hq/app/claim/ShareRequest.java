package net.officefloor.hq.app.claim;

/** A share as the screen sends it: the holder's person id and their percent (two decimals). */
public record ShareRequest(String personId, String share) {
}
