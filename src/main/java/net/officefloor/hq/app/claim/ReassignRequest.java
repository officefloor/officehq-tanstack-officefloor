package net.officefloor.hq.app.claim;

/**
 * What a reassign submits: the id of the person the claim moves to (null: none chosen), and whether it is one of a
 * bulk move (a bulk move counts a claim already that person's as moved, rather than refusing it).
 */
public record ReassignRequest(Long personId, Boolean bulk) {
}
