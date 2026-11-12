package net.officefloor.hq.app.paste;

import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.person.Person;

/** A pasted line that passed the claim form's rules: the named person, the claim to add them, and the
 *  id of the DRAFT it updates instead (null when it adds a claim). */
public record PastedClaim(Person person, ClaimInput claim, Long draftId) {
}
