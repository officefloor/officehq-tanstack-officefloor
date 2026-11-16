package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;

/** A pasted claim once checked: the person it is for, its values, and the rules' outcome. */
public record PastedClaim(Person person, ClaimValues values, ClaimRules.Checked checked) {
}
