package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Apply the edit to the loaded person and store it, refused when the person has been saved since
 *  the version the edit was made from. The grade is recorded from the edit's from-date (the sheet's today
 *  when none was given), keeping the grades already recorded from other dates. The person's own terms are set when the edit gives
 *  them (cleared when given blank), and so is their contact. */
public class SavePerson {
    public void save(@Val PersonRequest request, @Val Person loaded, PersonRepository repository,
            SheetRepository sheets, Out<PersonResponse> saved) throws StalePersonException {
        long from = request.version() == null ? loaded.version() : request.version();
        Person person = new Person(loaded.id(), loaded.sheetId(), request.name(), loaded.grade(), from + 1,
                loaded.statementCount(), loaded.statedTotal(), loaded.statementDate(), loaded.retiredFrom(),
                loaded.grades(), loaded.holds(), loaded.terms(), loaded.contact(), loaded.aliases(), loaded.origin(),
                loaded.retirements());
        if (!repository.update(person, from)) {
            throw new StalePersonException(loaded.name());
        }
        LocalDate gradeFrom = request.gradeFrom() != null ? LocalDate.parse(request.gradeFrom())
                : sheets.today(loaded.sheetId());
        repository.grade(person, request.grade(), gradeFrom);
        if (request.terms() != null) {
            repository.terms(person, request.terms().isEmpty() ? null : Integer.valueOf(request.terms()));
        }
        if (request.contact() != null) {
            repository.contact(person, request.contact().isEmpty() ? null : request.contact());
        }
        saved.set(PersonResponse.from(repository.find(loaded.sheetId(), loaded.id())));
    }
}
