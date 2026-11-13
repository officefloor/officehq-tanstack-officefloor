package net.officefloor.hq.app.claim;

/** Body of sharing a claim, as sent: the id of the person it is shared with, their share percent, and who shares it. */
public record ShareRequest(String person, String share, String user) {
}
