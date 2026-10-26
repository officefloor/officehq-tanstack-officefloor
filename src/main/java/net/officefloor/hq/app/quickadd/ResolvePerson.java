package net.officefloor.hq.app.quickadd;

import java.util.List;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.access.SettingRepository;
import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.claim.RefuseClosedNewDate;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.GradeFrom;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.PersonRequest;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * The person on the sheet with the typed name (case and surrounding spaces ignored), else a new one (refused when
 * another sheet of its tree uses the name). A new one is
 * added only once its claim's date is known not to fall in a closed period, so a refusal leaves no person behind.
 */
public class ResolvePerson {
    public void resolve(@Val SheetResponse sheet, @Val PersonRequest request, @Val ClaimInput input,
            PersonRepository repository, SheetRepository sheets, SettingRepository settings, PeriodRepository periods,
            HolidayRepository holidays, Out<Person> person, Out<QuickAddPerson> resolved) throws BadRequestException {
        Person found = repository.findByName(sheet.id(), request.name());
        boolean added = found == null;
        if (added) {
            if (repository.usedInTree(sheet.id(), request.name(), null)) {
                throw new BadRequestException(PersonRepository.NAME_TAKEN);
            }
            Person proposed = new Person(0, sheet.id(), request.name(), request.grade(), 0);
            proposed.setGrades(List.of(new GradeFrom(request.grade(), null)));
            new RefuseClosedNewDate().check(proposed, input, sheets, settings, periods, holidays);
            found = repository.insert(sheet.id(), request.name(), request.grade());
        }
        person.set(found);
        resolved.set(new QuickAddPerson(found, added));
    }
}
