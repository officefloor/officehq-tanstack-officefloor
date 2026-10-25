package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Refuses a new claim (date blank: the sheet's today) dated after its person's retirement date. */
public class RefuseRetiredDate {

    static final String RETIRED = "That claim's date is after the person's retirement date";

    public void check(@Val Person person, @Val ClaimInput input, SheetRepository sheets) throws BadRequestException {
        refuse(person, input.date() != null ? input.date() : sheets.today(person.getSheetId()));
    }

    static void refuse(Person person, LocalDate date) throws BadRequestException {
        if (person != null && person.isRetired() && date.isAfter(person.getRetiredFrom())) {
            throw new BadRequestException(RETIRED);
        }
    }
}
