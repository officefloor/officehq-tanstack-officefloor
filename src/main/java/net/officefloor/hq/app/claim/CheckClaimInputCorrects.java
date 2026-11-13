package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

/** Refuses (400) a correction unless the number it corrects is a SETTLED claim of the same person. */
public class CheckClaimInputCorrects {
    public void check(@Val ClaimInput input, @Val Person person, ClaimRepository claims) throws InvalidClaimException {
        if (input.corrects() == null) {
            return;
        }
        boolean settled = claims.findForPerson(person.sheetId(), person.id()).stream()
                .anyMatch(c -> input.corrects().equals(c.number()) && c.status() == ClaimStatus.SETTLED);
        if (!settled) {
            throw new InvalidClaimException(
                    "A correction must name a settled claim of " + person.name() + "; no settled claim " + input.corrects() + ".");
        }
    }
}
