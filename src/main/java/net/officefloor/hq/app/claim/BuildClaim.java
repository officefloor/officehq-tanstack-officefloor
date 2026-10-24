package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonGradeRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Adds the claim, fixing its due-by: its date plus its person's own terms, else the terms of their grade in force on its date; refuses one dated in a closed period, or after the person's retirement date, and a correction
 * that does not name a settled claim of the person. */
public class BuildClaim {
    public void build(@Val ClaimInput input, @Val Sheet sheet, @Val Person person, ClaimRepository repository,
            PeriodRepository periods, PersonGradeRepository grades, Out<Claim> claimOut) throws InvalidClaimException {
        claimOut.set(repository.save(ClaimRules.make(input, sheet, person, repository, periods, grades)));
    }
}
