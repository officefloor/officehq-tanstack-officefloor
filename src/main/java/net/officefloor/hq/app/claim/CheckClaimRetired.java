package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;

/** Refuses the change when the claim, as it stands, is dated after its person's retirement; a claim
 *  dated on or before it is allowed, however late it is added. */
public class CheckClaimRetired {
    public void check(@Val Claim claim, PersonRepository people) throws RetiredPersonException {
        var person = claim.getPersonId() == null ? null : people.findById(claim.getPersonId()).orElse(null);
        if (person != null && person.getRetiredFrom() != null && claim.getDate().isAfter(person.getRetiredFrom())) {
            throw new RetiredPersonException(person.getName() + " is retired from " + person.getRetiredFrom()
                    + "; a claim cannot be dated after that.");
        }
    }
}
