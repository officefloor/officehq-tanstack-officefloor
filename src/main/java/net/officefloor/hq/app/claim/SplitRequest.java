package net.officefloor.hq.app.claim;

/** Body of splitting a claim: the amount split off it, and who splits it. */
public record SplitRequest(String amount, String by) {
}
