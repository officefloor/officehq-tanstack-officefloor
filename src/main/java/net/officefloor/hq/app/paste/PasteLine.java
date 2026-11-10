package net.officefloor.hq.app.paste;

import net.officefloor.hq.app.claim.ClaimInput;

/** One pasted line checked by the claim form's rules: the trimmed name of its person and its claim. */
public record PasteLine(String name, ClaimInput claim) {
}
