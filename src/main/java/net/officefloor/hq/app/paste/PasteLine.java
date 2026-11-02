package net.officefloor.hq.app.paste;

import net.officefloor.hq.app.claim.ClaimInput;

/** One pasted line, bound by the claim form's rules: the person's name (trimmed) and their claim. */
public record PasteLine(String name, ClaimInput claim) {
}
