package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;

/** One checked line of a paste: the person it names and its claim. */
public record PastedLine(Person person, ClaimInput input) {
}
