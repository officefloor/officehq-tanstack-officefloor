package net.officefloor.hq.app.claim;

/** A stored status change: the number the claim carried through it (given on opening, or held until
 *  voided), null when it had none. */
public record StatusChange(Long number) {
}
