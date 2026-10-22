package net.officefloor.hq.app.claim;

/** One pasted line, checked by the claim form's rules: its number in the box, the person's name as typed and
 *  the claim to add for them. */
public record PasteLine(int number, String name, ClaimInput claim) {
}
