package net.officefloor.hq.app.claim;

/** A share of a claim to give: the person (by id) and their share (a percent with up to two decimals). */
public record ShareRequest(Long person, String share) {
}
