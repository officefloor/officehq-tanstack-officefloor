package net.officefloor.hq.app.claim;

/** The body of a share: {@code personId} is the person on the sheet to share with, {@code share} their percent (two decimals). */
public record ShareRequest(Long personId, String share) {
}
