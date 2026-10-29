package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class ApplyPerson {
    // Person is a record, so the edited person is published in place of the loaded one. The edit is
    // refused when the version its form loaded is not the stored one: someone else saved it since.
    // The form's grade is given from its from-date (the sheet's today when blank), keeping the grades
    // already given; a save leaving the grade in force today as it is, with no from-date, gives none.
    // The form's terms are recorded from its terms from-date, keeping (or replacing, for the same date) those already
    // recorded; with no from-date they are recorded from the beginning of time, unless they are the terms in force today.
    // The name is refused when anyone else on the sheet's tree uses it as a name or alias.
    // A retired grade is refused, unless the save leaves it as the grade in force today, with no from-date.
    public void apply(@Val PersonRequest request, @Val Person person, People people, Sheets sheets,
            net.officefloor.hq.app.grade.Grades grades, Out<PersonResponse> responseOut) throws InvalidPersonException, PersonConflictException {
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
        if (grades.retired(person.sheetId(), request.grade())
                && (request.gradeFrom() != null || !request.grade().equals(person.gradeOn(today)))) {
            throw new InvalidPersonException("Grade " + request.grade() + " is retired.");
        }
        Integer terms = request.terms() == null ? null : Integer.valueOf(request.terms());
        String fromStart = request.termsFrom() == null && !Objects.equals(terms, person.ownTermsOn(today)) ? request.terms()
                : person.terms() == null ? null : person.terms().toString();
        Person edited = person.with(new PersonRequest(request.name(), request.grade(), request.version(), request.gradeFrom(),
                fromStart, request.contact(), request.tags(), request.termsFrom()));
        if (!people.save(edited)) {
            throw new PersonConflictException("Someone else has saved this person since it was opened.");
        }
        if (request.termsFrom() != null) {
            LocalDate from = LocalDate.parse(request.termsFrom());
            people.termsFrom(person.id(), from, terms);
            List<DatedTerms> datedTerms = new ArrayList<>(edited.datedTerms().stream().filter(d -> !d.from().equals(from)).toList());
            datedTerms.add(new DatedTerms(from, terms));
            datedTerms.sort(Comparator.comparing(DatedTerms::from));
            edited = new Person(edited.id(), edited.sheetId(), edited.name(), edited.grade(), edited.version(),
                    edited.retiredFrom(), edited.holds(), edited.dated(), edited.terms(), edited.contact(), edited.aliases(), edited.origin(), edited.retirements(), edited.tags(), edited.snapshots(), List.copyOf(datedTerms));
        }
        if (request.gradeFrom() != null || !request.grade().equals(person.gradeOn(today))) {
            LocalDate from = request.gradeFrom() != null ? LocalDate.parse(request.gradeFrom()) : today;
            people.gradeFrom(person.id(), from, request.grade());
            List<DatedGrade> dated = new ArrayList<>(edited.dated().stream().filter(d -> !d.from().equals(from)).toList());
            dated.add(new DatedGrade(from, request.grade()));
            dated.sort(Comparator.comparing(DatedGrade::from));
            edited = new Person(edited.id(), edited.sheetId(), edited.name(), edited.grade(), edited.version(),
                    edited.retiredFrom(), edited.holds(), List.copyOf(dated), edited.terms(), edited.contact(), edited.aliases(), edited.origin(), edited.retirements(), edited.tags(), edited.snapshots(), edited.datedTerms());
        }
        responseOut.set(PersonResponse.from(edited, today));
    }
}
