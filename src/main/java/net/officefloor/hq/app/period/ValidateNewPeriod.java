package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Refuse a period whose name another period on the sheet already has (without regard to case), or whose
 *  dates overlap another period's. */
public class ValidateNewPeriod {
    public void validate(@PathVariable("sheetId") String sheetId, @Val PeriodInput input,
            PeriodRepository repository) throws InvalidPeriodException {
        String id = sheetId.trim();
        repository.lockSheet(id);
        for (Period p : repository.list(id)) {
            if (p.name().trim().equalsIgnoreCase(input.name())) {
                throw new InvalidPeriodException("The period name " + input.name() + " is already in use on this sheet.");
            }
            if (p.overlaps(input.from(), input.to())) {
                throw new InvalidPeriodException("The dates overlap the period " + p.name() + ".");
            }
        }
    }
}
