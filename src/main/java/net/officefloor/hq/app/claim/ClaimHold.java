package net.officefloor.hq.app.claim;

/** A hold or release of a claim once its body is read: whether it is to be held, and who does it. */
public record ClaimHold(boolean held, String by) {
}
