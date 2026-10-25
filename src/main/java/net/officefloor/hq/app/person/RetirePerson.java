package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Retires the person from the date (blank: the sheet's today); refuses a person already retired. */
public class RetirePerson {
    public void retire(@Val RetireInput input, @Val Person person, PersonRepository repository, SheetRepository sheets)
            throws BadRequestException {
        if (person.isRetired()) {
            throw new BadRequestException("That person is already retired");
        }
        LocalDate from = input.date() != null ? input.date() : sheets.today(person.getSheetId());
        if (!repository.retire(person.getId(), from)) {
            throw new BadRequestException("That person was retired by someone else; reload the sheet");
        }
        person.setRetiredFrom(from);
    }
}
