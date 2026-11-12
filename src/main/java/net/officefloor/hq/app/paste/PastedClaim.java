package net.officefloor.hq.app.paste;

import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.person.Person;

/** A pasted line that passed the claim form's rules: the named person and the claim to add them. */
public record PastedClaim(Person person, ClaimInput claim) {
}
