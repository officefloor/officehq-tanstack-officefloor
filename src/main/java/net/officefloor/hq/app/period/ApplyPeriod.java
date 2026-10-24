package net.officefloor.hq.app.period;

import java.util.List;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Adds the period to the sheet; refuses a name the sheet already has and dates overlapping another period's. */
public class ApplyPeriod {
    public void apply(@Val PeriodInput input, @Val Sheet sheet, PeriodRepository repository, Out<Period> periodOut)
            throws InvalidPeriodException {
        List<Period> periods = repository.findBySheetIdOrderByFromDateAscIdAsc(sheet.getId());
        if (periods.stream().anyMatch(p -> p.getName().equals(input.name()))) {
            throw new InvalidPeriodException("The sheet already has a period with that name.");
        }
        if (periods.stream().anyMatch(p -> p.overlaps(input.from(), input.to()))) {
            throw new InvalidPeriodException("The period overlaps another period.");
        }
        periodOut.set(repository.saveAndFlush(new Period(sheet.getId(), input.name(), input.from(), input.to())));
    }
}
