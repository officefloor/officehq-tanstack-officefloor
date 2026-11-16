package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Retires the person from the date; a retirement given no date is from the sheet's today. */
public class RetirePerson {
    public void retire(@Val Person person, @Val Retirement retirement, SheetRepository sheets, PersonRepository people,
            Out<Person> retired) {
        LocalDate from = retirement.from() != null ? retirement.from() : sheets.today(person.sheetId());
        people.retire(person.sheetId(), person.id(), from);
        retired.set(new Person(person.id(), person.sheetId(), person.name(), person.grade(), person.version(), from.toString(),
                person.grades(), person.holds(), person.terms(), person.contact(), person.aliases())
                .withRetirements(person.retirements()));
    }
}
