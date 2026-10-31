package net.officefloor.hq.app.claim;

/** Body of a claim's split: the amount to split off it (more than 0.00, less than its amount), and who splits it. */
public record ClaimSplitRequest(String amount, String user) {
}
