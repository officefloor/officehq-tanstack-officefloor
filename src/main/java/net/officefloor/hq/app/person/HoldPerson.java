package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Adds a hold to the person, from its from-date (blank: the sheet's today) to its to-date; refuses one ending before it starts. */
public class HoldPerson {
    public void hold(@Val HoldInput input, @Val Person person, PersonRepository repository, SheetRepository sheets,
            Out<HoldSpan> added) throws BadRequestException {
        LocalDate from = input.from() != null ? input.from() : sheets.today(person.getSheetId());
        if (input.to() != null && input.to().isBefore(from)) {
            throw new BadRequestException("A hold's to-date cannot be before its from-date");
        }
        HoldSpan hold = new HoldSpan(from, input.to());
        repository.addHold(person.getId(), hold);
        List<HoldSpan> holds = new ArrayList<>(person.getHolds());
        holds.add(hold);
        person.setHolds(holds);
        added.set(hold);
    }
}
