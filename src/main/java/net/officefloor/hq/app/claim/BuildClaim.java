package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Adds the claim; refuses one dated in a closed period, or after the person's retirement date. */
public class BuildClaim {
    public void build(@Val ClaimInput input, @Val Sheet sheet, @Val Person person, ClaimRepository repository,
            PeriodRepository periods, Out<Claim> claimOut) throws InvalidClaimException {
        LocalDate date = input.date() != null ? input.date() : sheet.getToday();
        if (periods.isClosedOn(sheet.getId(), date)) {
            throw new InvalidClaimException("No claim can be added in a closed period.");
        }
        if (person.retiredOn(date)) {
            throw new InvalidClaimException("No claim can be dated after the person's retirement date.");
        }
        claimOut.set(repository.save(new Claim(sheet.getId(), person.getId(), input.amount(), date, input.note())));
    }
}
