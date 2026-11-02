package net.officefloor.hq.app.paste;

import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.person.Person;

/** One checked pasted line: its line number in the paste, the person it names and the claim it takes for them. */
public record PastedClaim(int at, Person person, ClaimInput input) {
}
