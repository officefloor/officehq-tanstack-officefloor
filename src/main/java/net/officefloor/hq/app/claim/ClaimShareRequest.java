package net.officefloor.hq.app.claim;

/** Body of sharing a claim: who shares it, the person on the sheet it is shared with and the share
 *  (a percent with up to two decimals) of its due they hold. */
public record ClaimShareRequest(String user, Long personId, String share) {
}
