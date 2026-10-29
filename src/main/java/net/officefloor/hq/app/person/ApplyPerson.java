package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class ApplyPerson {
    // Person is a record, so the edited person is published in place of the loaded one. The edit is
    // refused when the version its form loaded is not the stored one: someone else saved it since.
    // The form's grade is given from its from-date (the sheet's today when blank), keeping the grades
    // already given; a save leaving the grade in force today as it is, with no from-date, gives none.
    // The name is refused when anyone else on the sheet's tree uses it as a name or alias.
    public void apply(@Val PersonRequest request, @Val Person person, People people, Sheets sheets,
            Out<PersonResponse> responseOut) throws InvalidPersonException, PersonConflictException {
        if (request.version() != null && request.version() != person.version()) {
            throw new PersonConflictException("Someone else has saved this person since it was opened.");
        }
        if (people.nameTaken(person.sheetId(), request.name(), person.id())) {
            throw new InvalidPersonException("The name is already in use.");
        }
        // A person whose alias clashes across the sheet's tree is refused every edit (only a rename frees the name).
        if (people.aliasClashes(person)) {
            throw new InvalidPersonException("An alias of theirs is already in use on the sheet's tree.");
        }
        if (request.contact() != null && people.contactTaken(person.sheetId(), request.contact(), person.id())) {
            throw new InvalidPersonException("The contact is already in use.");
        }
        LocalDate today = sheets.today(person.sheetId());
        Person edited = person.with(request);
        if (!people.save(edited)) {
            throw new PersonConflictException("Someone else has saved this person since it was opened.");
        }
        if (request.gradeFrom() != null || !request.grade().equals(person.gradeOn(today))) {
            LocalDate from = request.gradeFrom() != null ? LocalDate.parse(request.gradeFrom()) : today;
            people.gradeFrom(person.id(), from, request.grade());
            List<DatedGrade> dated = new ArrayList<>(edited.dated().stream().filter(d -> !d.from().equals(from)).toList());
            dated.add(new DatedGrade(from, request.grade()));
            dated.sort(Comparator.comparing(DatedGrade::from));
            edited = new Person(edited.id(), edited.sheetId(), edited.name(), edited.grade(), edited.version(),
                    edited.retiredFrom(), edited.holds(), List.copyOf(dated), edited.terms(), edited.contact(), edited.aliases(), edited.origin(), edited.retirements(), edited.tags(), edited.snapshots());
        }
        responseOut.set(PersonResponse.from(edited, today));
    }
}
