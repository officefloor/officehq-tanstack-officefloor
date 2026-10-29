package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;

/**
 * One checked line of a paste: the person it names and its claim, and the person's DRAFT claim it updates (its date and
 * note those of the line, as the claims stood before the paste); null when it adds a claim.
 */
public record PastedLine(Person person, ClaimInput input, Claim draft) {
}
