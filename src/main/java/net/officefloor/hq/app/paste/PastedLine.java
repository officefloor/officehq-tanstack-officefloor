package net.officefloor.hq.app.paste;

import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.person.Person;

/** One pasted line: the name it typed, its claim as the claim form validates it, and (once found) its person. */
public record PastedLine(String name, ClaimInput input, Person person) {
}
