package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Add a hold to the loaded person, from the date given (the sheet's today when none was) to the to-date given
 *  (none: open-ended), keeping the holds they already have. Refused when the to-date is before the sheet's today
 *  taken as the from-date. */
public class SaveHold {
    public void save(@Val HoldInput input, @Val Person loaded, SheetRepository sheets, PersonRepository repository,
            Out<Holds.Hold> hold, Out<PersonResponse> saved) throws InvalidPersonException {
        LocalDate from = input.from() != null ? input.from() : sheets.today(loaded.sheetId());
        if (input.to() != null && input.to().isBefore(from)) {
            throw new InvalidPersonException("The hold's to-date must not be before its from-date.");
        }
        repository.hold(loaded, from, input.to());
        hold.set(new Holds.Hold(from, input.to()));
        saved.set(PersonResponse.from(repository.find(loaded.sheetId(), loaded.id())));
    }
}
