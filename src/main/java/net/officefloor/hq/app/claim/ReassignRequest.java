package net.officefloor.hq.app.claim;

/** A move of a claim as the screen sends it: the id of the person it moves to. */
public record ReassignRequest(String personId) {
}
