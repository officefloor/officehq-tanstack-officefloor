package net.officefloor.hq.app.opening;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Finds each pasted line's person (by name or alias) and period on the sheet; one not found refuses the whole paste. */
public class CheckImportOpenings {
    public void check(@Val SheetResponse sheet, @Val TypedOpenings typed, PersonRepository people,
            PeriodRepository periods, Out<ImportedOpenings> imported) throws BadRequestException {
        List<Opening> openings = new ArrayList<>();
        for (OpeningLine line : typed.lines()) {
            Person person = people.findByName(sheet.id(), line.name());
            if (person == null) {
                throw new BadRequestException("Line " + line.line() + ": no one on this sheet is named " + line.name());
            }
            Period period = periods.findByName(sheet.id(), line.period());
            if (period == null) {
                throw new BadRequestException("Line " + line.line() + ": no period on this sheet is named " + line.period());
            }
            openings.add(new Opening(person, period, line.amount()));
        }
        imported.set(new ImportedOpenings(openings));
    }
}
