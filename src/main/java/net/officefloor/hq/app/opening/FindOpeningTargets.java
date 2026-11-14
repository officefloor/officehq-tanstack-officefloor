package net.officefloor.hq.app.opening;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Finds each line's person by name or alias (ignoring case) and its period by name on the sheet. A line naming
 * either not on the sheet refuses the whole import, before anything is written.
 */
public class FindOpeningTargets {
    public void find(@Val PastedOpenings pasted, @Val Sheet sheet, PersonRepository people, PeriodRepository periods,
            Out<FoundOpenings> found) throws InvalidOpeningException {
        List<PastedOpening> lines = new ArrayList<>();
        int number = 0;
        for (PastedOpening line : pasted.lines()) {
            number++;
            Person person = people.findByName(sheet.id(), line.name());
            if (person == null) {
                throw new InvalidOpeningException("Line " + number + ": " + line.name() + " is not on the sheet.");
            }
            Period period = periods.find(sheet.id(), line.periodName());
            if (period == null) {
                throw new InvalidOpeningException("Line " + number + ": the period " + line.periodName()
                        + " is not on the sheet.");
            }
            lines.add(new PastedOpening(line.name(), line.periodName(), line.amount(), person, period));
        }
        found.set(new FoundOpenings(lines));
    }
}
