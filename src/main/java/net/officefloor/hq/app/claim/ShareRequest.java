package net.officefloor.hq.app.claim;

/** Body of sharing a claim: the person to share it with, their share (a percent with up to two decimals) and who shares it. */
public record ShareRequest(Long personId, String share, String by) {
}
