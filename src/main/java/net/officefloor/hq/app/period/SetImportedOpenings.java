package net.officefloor.hq.app.period;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Resolves every line first (the person on the sheet by name or alias, the period on the sheet), so
 * one refused line writes nothing; then adds each amount to the person's opening balance for the
 * period, in order, auditing it (OPENING_SET, with the person's own name).
 */
public class SetImportedOpenings {
    public void set(@Val Sheet sheet, @Val ImportOpeningsLines lines, PersonRepository people,
            PeriodRepository periods, HttpServletRequest request, Audit audit, Out<ImportOpeningsResponse> result)
            throws InvalidPeriodException {
        List<Person> persons = new ArrayList<>();
        List<Period> found = new ArrayList<>();
        int number = 0;
        for (OpeningLine line : lines.lines()) {
            number++;
            Person person = people.findByName(sheet.id(), line.name());
            if (person == null) {
                throw new InvalidPeriodException("Line " + number + ": no one named " + line.name() + " is on the sheet");
            }
            Period period = periods.find(sheet.id(), line.period());
            if (period == null) {
                throw new InvalidPeriodException("Line " + number + ": no period " + line.period() + " is on the sheet");
            }
            persons.add(person);
            found.add(period);
        }
        String user = Audit.user(request);
        for (int i = 0; i < persons.size(); i++) {
            Person person = persons.get(i);
            Period period = found.get(i);
            OpeningLine line = lines.lines().get(i);
            periods.addOpening(sheet.id(), period.name(), person.getId(), line.amount());
            audit.record("OPENING_SET sheet=" + sheet.id() + " by=" + user + " name=" + person.getName()
                    + " period=" + period.name() + " amount=" + line.amount().setScale(2).toPlainString());
        }
        result.set(new ImportOpeningsResponse(persons.size()));
    }
}
