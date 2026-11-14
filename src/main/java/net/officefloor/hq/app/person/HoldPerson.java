package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Adds a hold on the person from the date asked (the sheet's today when none) to the to-date asked
 *  (none: open-ended); a to-date before a defaulted from-date is refused. */
public class HoldPerson {
    public void hold(@RequestParam(name = "by", required = false) String by, @Val HoldRequest request,
            @Val Person person, SheetRepository sheets, PersonRepository people)
            throws InvalidPersonException {
        String from = request.from() != null ? request.from() : sheets.today(person.getSheetId()).toString();
        if (request.to() != null && request.to().compareTo(from) < 0) {
            throw new InvalidPersonException("The hold ends before it starts: " + from + " to " + request.to());
        }
        people.addHold(person, new Hold(from, request.to()), by == null ? "" : by);
    }
}
