package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Unretires the person on the sheet's today, keeping the retirement as a past one; refuses anyone not retired. */
public class UnretirePerson {
    public void unretire(@Val Person person, PersonRepository repository, SheetRepository sheets,
            Out<UnretiredOn> unretired) throws BadRequestException {
        if (!person.isRetired()) {
            throw new BadRequestException("That person is not retired");
        }
        LocalDate on = sheets.today(person.getSheetId());
        if (!repository.unretire(person.getId(), person.getRetiredFrom(), on)) {
            throw new BadRequestException("That person was changed by someone else; reload the sheet");
        }
        List<RetirementSpan> past = new ArrayList<>(person.getRetirements());
        past.add(new RetirementSpan(person.getRetiredFrom(), on));
        person.setRetirements(past);
        person.setRetiredFrom(null);
        unretired.set(new UnretiredOn(on));
    }
}
