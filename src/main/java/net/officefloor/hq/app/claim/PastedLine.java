package net.officefloor.hq.app.claim;

/** One pasted line: the person's name as typed and the claim's values as the claim form checks them. */
public record PastedLine(String name, ClaimValues values) {
}
