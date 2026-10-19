package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Adds a hold to the person, from the requested date (the sheet's today without one) to the optional
 *  to-date. A to-date before the from-date (the sheet's today) is refused. */
public class HoldPerson {
    public void hold(@Val HoldRequest request, @Val Person person, SheetClock clock, PersonRepository repository,
            Out<Hold> added) throws InvalidPersonException {
        LocalDate from = request.from().isEmpty() ? clock.today(person.getSheetId()) : LocalDate.parse(request.from());
        LocalDate to = request.to().isEmpty() ? null : LocalDate.parse(request.to());
        if (to != null && to.isBefore(from)) throw new InvalidPersonException("The hold cannot end before it starts.");
        Hold hold = new Hold(from, to);
        repository.addHold(person, hold);
        person.getHolds().add(hold);
        added.set(hold);
    }
}
