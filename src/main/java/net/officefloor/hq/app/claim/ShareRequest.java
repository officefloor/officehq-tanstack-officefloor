package net.officefloor.hq.app.claim;

/** Body of a claim share: who is making it, the id of the person taking the share and their percent. */
public record ShareRequest(String user, String personId, String share) {
}
