package net.officefloor.hq.app.claim;

/** One pasted line: the person's name as given, and the claim's values by the claim form's rules. */
public record PastedLine(String name, ClaimValues values) {
}
