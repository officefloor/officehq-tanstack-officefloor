package net.officefloor.hq.app.paste;

import net.officefloor.hq.app.claim.ValidClaim;

/** A pasted line once checked as the claim form checks it: the person's name (trimmed) and the claim. */
public record PasteLine(String name, ValidClaim claim) {
}
