package net.officefloor.hq.app.claim;

/** One pasted line: the person's name as typed and the claim's fields under the claim form's rules. */
public record PasteLine(String name, ClaimInput input) {
}
