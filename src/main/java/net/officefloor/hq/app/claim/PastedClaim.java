package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;

/** A pasted claim once checked: the person it is for, its values, the rules' outcome, and the DRAFT the line updates
 *  (null when it adds a claim). */
public record PastedClaim(Person person, ClaimValues values, ClaimRules.Checked checked, Claim draft) {
}
