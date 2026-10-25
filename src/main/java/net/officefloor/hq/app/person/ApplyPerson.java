package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.hq.app.ConflictException;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Saves the edit, refusing it (nothing written) when the person was saved by someone else since
 *  the form loaded it. The grade is given from the request's from-date (the sheet's today when blank), keeping the
 *  grades already given; the person then shows the grade in force on the sheet's today. */
public class ApplyPerson {
    public void apply(@Val PersonRequest request, @Val Person person, PersonRepository repository,
            SheetRepository sheets) throws ConflictException {
        if (request.version() == null || request.version() != person.getVersion()) {
            throw new ConflictException(PersonRepository.CHANGED);
        }
        person.setName(request.name());
        if (!repository.update(person)) {
            throw new ConflictException(PersonRepository.CHANGED);
        }
        LocalDate today = sheets.today(person.getSheetId());
        LocalDate from = request.gradeFrom() == null || request.gradeFrom().isEmpty() ? today
                : LocalDate.parse(request.gradeFrom());
        repository.giveGrade(person, request.grade(), from, today);
    }
}
