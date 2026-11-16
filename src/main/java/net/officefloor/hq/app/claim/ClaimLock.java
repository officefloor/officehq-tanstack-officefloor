package net.officefloor.hq.app.claim;

/** Who opens or closes a claim's editor. */
public record ClaimLock(String by) {
}
