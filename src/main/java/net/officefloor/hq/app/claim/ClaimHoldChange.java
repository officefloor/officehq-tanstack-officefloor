package net.officefloor.hq.app.claim;

/** Whether holding or releasing the claim changes it: false when it already was as asked (nothing is written). */
public record ClaimHoldChange(boolean changed) {
}
