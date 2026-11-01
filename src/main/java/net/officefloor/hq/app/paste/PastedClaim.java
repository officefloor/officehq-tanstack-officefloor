package net.officefloor.hq.app.paste;

import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.person.Person;

/** One checked pasted line: the person it names and the claim to add for them. */
public record PastedClaim(Person person, ClaimInput input) {
}
