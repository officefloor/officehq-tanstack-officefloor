package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Adds a hold to the person, from the date given or the sheet's today, to the date given (or
 *  open-ended). Read afresh and locked, so two holds at once happen one after the other. Not an
 *  edit: the person's version is untouched, so an editor open on them can still save. Publishes
 *  the person as they are now, and the hold added. */
public class HoldPerson {
    public void hold(@PathVariable("sheetId") String sheetId, @PathVariable("personId") long personId,
            @Val HoldChange change, People people, Sheets sheets, Out<Person> personOut, Out<Hold> holdOut)
            throws PersonNotFoundException, InvalidSheetRequestException {
        Person person = people.lock(sheetId, personId);
        if (person == null) throw new PersonNotFoundException(personId);
        LocalDate from = change.from() != null ? change.from() : sheets.today(sheetId);
        if (change.to() != null && change.to().isBefore(from)) {
            throw new InvalidSheetRequestException("A hold's to-date cannot be before its from-date");
        }
        Hold hold = new Hold(from, change.to());
        people.addHold(person, hold);
        personOut.set(person);
        holdOut.set(hold);
    }
}
