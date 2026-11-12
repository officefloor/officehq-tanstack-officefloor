package net.officefloor.hq.app.claim;

/** A stored status change: the number the claim carried through it (given on settling, and kept
 *  when voided), null when it had none. */
public record StatusChange(String number) {
}
