package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

/** Refuses (400) an edited correction on the terms of {@link CheckClaimInputCorrects}, leaving the edited claim out of what it is limited by. */
public class CheckClaimEditCorrects {
    public void check(@Val ClaimInput input, @Val Person person, @Val Claim claim, ClaimRepository claims) throws InvalidClaimException {
        CheckClaimInputCorrects.check(input, person, claims, claim.id());
    }
}
