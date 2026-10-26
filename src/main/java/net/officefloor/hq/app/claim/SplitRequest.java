package net.officefloor.hq.app.claim;

/** The body of a split: {@code amount} is the amount (up to two decimals) split off the claim. */
public record SplitRequest(String amount) {
}
