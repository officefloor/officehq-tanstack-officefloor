package net.officefloor.hq.app.paste;

import net.officefloor.hq.app.claim.ClaimInput;

/** One pasted line once checked by the claim form's rules: the trimmed name and the claim it makes. */
public record PastedLine(String name, ClaimInput claim) {
}
