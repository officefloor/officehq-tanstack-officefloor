package net.officefloor.hq.app.claim;

/** Body of a split: who is making it and the amount to split off the claim. */
public record SplitRequest(String user, String amount) {
}
