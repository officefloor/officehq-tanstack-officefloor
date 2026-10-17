package net.officefloor.hq.app.period;

import java.util.List;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Builds the period on the sheet, refusing a name already used there or dates overlapping another period. */
public class BuildPeriod {
    public void build(@PathVariable("sheetId") String sheetId, @Val PeriodInput input, PeriodRepository repository,
            Out<Period> periodOut) throws InvalidPeriodException {
        String id = sheetId.trim();
        List<Period> periods = repository.findBySheetId(id);
        if (periods.stream().anyMatch((p) -> p.getName().equals(input.name()))) {
            throw new InvalidPeriodException("A period named " + input.name() + " is already on the sheet.");
        }
        for (Period p : periods) {
            if (p.overlaps(input.from(), input.to())) {
                throw new InvalidPeriodException("The period overlaps " + p.getName() + ".");
            }
        }
        periodOut.set(new Period(id, input.name(), input.from(), input.to()));
    }
}
