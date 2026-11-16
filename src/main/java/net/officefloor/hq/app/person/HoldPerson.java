package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Adds a hold on the person from the from-date (the sheet's today when blank) until the to-date, if any; a to-date
 *  before the from-date is refused. Publishes the hold as added (its from-date filled in). */
public class HoldPerson {
    public void hold(@Val Person person, @Val Hold hold, SheetRepository sheets, PersonRepository people,
            Out<Person> held, Out<PersonHold> added) throws InvalidPersonException {
        LocalDate from = hold.from() != null ? hold.from() : sheets.today(person.sheetId());
        if (hold.to() != null && hold.to().isBefore(from)) {
            throw new InvalidPersonException("The hold's to-date must not be before its from-date");
        }
        PersonHold span = new PersonHold(from, hold.to());
        people.hold(person.sheetId(), person.id(), span);
        List<PersonHold> holds = new ArrayList<>(person.holds());
        holds.add(span);
        holds.sort(Comparator.comparing(PersonHold::from));
        held.set(new Person(person.id(), person.sheetId(), person.name(), person.grade(), person.version(),
                person.retiredFrom(), person.grades(), List.copyOf(holds), person.terms(), person.contact(), person.aliases())
                .withRetirements(person.retirements()));
        added.set(span);
    }
}
