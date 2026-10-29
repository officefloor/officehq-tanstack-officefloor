package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class HoldPerson {
    // Adds a hold to the person's holds (a blank from-date is the sheet's today); a hold ends on its to-date.
    // Person is a record, so the held person is published as the response in place of the loaded one.
    public void hold(@Val HoldDates dates, @Val Person person, People people, Sheets sheets,
            Out<PersonResponse> responseOut) throws InvalidPersonException {
        LocalDate today = sheets.today(person.sheetId());
        Hold hold = new Hold(dates.from() != null ? dates.from() : today, dates.to());
        if (hold.to() != null && hold.to().isBefore(hold.from())) {
            throw new InvalidPersonException("The to-date is before the from-date.");
        }
        people.hold(person.id(), hold);
        List<Hold> holds = new ArrayList<>(person.holds());
        holds.add(hold);
        holds.sort(Comparator.comparing(Hold::from));
        Person held = new Person(person.id(), person.sheetId(), person.name(), person.grade(), person.version(),
                person.retiredFrom(), List.copyOf(holds), person.dated(), person.terms(), person.contact(), person.aliases(), person.origin(), person.retirements());
        responseOut.set(PersonResponse.from(held, today));
    }
}
