package net.officefloor.hq.app.opening;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Check every line against the sheet before anything is written: its person is on the sheet by name or alias, and
 *  its period is on the sheet (both without regard to case or surrounding spaces). One failing line refuses all. */
public class CheckOpenings {
    public void check(@PathVariable("sheetId") String sheetId, @Val OpeningImportInput input, PersonRepository people,
            PeriodRepository periods, Out<Openings> checked) throws InvalidOpeningException {
        String sheet = sheetId.trim();
        List<Opening> openings = new ArrayList<>();
        for (OpeningLine line : input.lines()) {
            Person person = people.findByName(sheet, line.name());
            if (person == null) {
                throw new InvalidOpeningException("Line " + line.number() + ": no one on the sheet is called "
                        + line.name() + ".");
            }
            Period period = periods.find(sheet, line.period());
            if (period == null) {
                throw new InvalidOpeningException("Line " + line.number() + ": there is no period " + line.period()
                        + " on the sheet.");
            }
            openings.add(new Opening(person.id(), person.name(), period.name(), line.amount()));
        }
        checked.set(new Openings(openings));
    }
}
