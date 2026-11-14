package net.officefloor.hq.app.claim;

/** What a share submits: the id of the person given the share (null: none chosen) and the percent (two decimals). */
public record ShareRequest(Long personId, String share) {
}
