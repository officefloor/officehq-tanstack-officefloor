package net.officefloor.hq.app.claim;

/** A void as the screen sends it: the reason chosen from the sheet's list ('' or null: none). */
public record VoidRequest(String reason) {
}
