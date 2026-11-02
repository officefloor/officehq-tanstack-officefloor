package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class SavePerson {
    public void save(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val Person person,
            @Val PersonRequest request, People people, SheetClock clock, Audit audit, Rollup rollup)
            throws DuplicatePersonNameException, StalePersonException, InvalidPersonException {
        // Through the form, a name or alias of theirs used on another sheet of the tree refuses the edit: a clashing
        // person may only be renamed out of the clash, and only when no alias of theirs clashes either.
        if (people.usedInTree(sheet, request.name()) || people.aliasClashes(sheet, person.id())) {
            throw new DuplicatePersonNameException(request.name());
        }
        apply(sheet, user, person, request, people, clock, audit, rollup);
    }

    /** Saves the person as the form gave them, auditing the edit and the rollup it moves; shared with the bulk grade. */
    static void apply(String sheet, String user, Person person, PersonRequest request, People people, SheetClock clock,
            Audit audit, Rollup rollup) throws DuplicatePersonNameException, StalePersonException, InvalidPersonException {
        // Saving over a version other than the one the editor loaded is refused, and writes nothing.
        long loaded = request.version() == null ? person.version() : request.version();
        if (loaded != person.version()) {
            throw new StalePersonException(person.id());
        }
        // A contact another person on the sheet already has is refused, and writes nothing.
        String contact = request.contactOrNull();
        if (contact != null && people.contactTaken(sheet, contact, person.id())) {
            throw new InvalidPersonException("The contact is already in use: " + contact);
        }
        Rollup.Snapshot before = rollup.take(sheet);
        people.update(sheet, new Person(person.id(), request.name(), person.grade(), loaded, person.retiredFrom(),
                person.holds(), person.grades(), person.terms(), person.retiredNow(), person.contact(), person.clashing(),
                person.origin()));
        // The form's grade is recorded from its from-date (the sheet's today when blank), keeping the ones already
        // recorded; leaving the grade as the row showed it, with the from-date blank, records nothing.
        if (request.gradeFrom() != null || !request.grade().equals(person.grade())) {
            LocalDate from = request.gradeFrom() == null ? clock.today(sheet) : LocalDate.parse(request.gradeFrom());
            people.setGrade(person.id(), request.grade(), from);
        }
        // The terms are recorded as the form gave them; only a change from what the row showed is audited.
        Integer terms = request.termsDays();
        boolean termsChanged = !java.util.Objects.equals(terms, person.terms());
        if (termsChanged) {
            people.setTerms(person.id(), terms);
        }
        // Likewise the contact; only a change from the one stored is audited (empty when it was cleared).
        boolean contactChanged = !java.util.Objects.equals(contact, person.contact());
        if (contactChanged) {
            people.setContact(person.id(), contact);
        }
        Person saved = people.find(sheet, person.id());
        audit.record("PERSON_EDITED sheet=" + sheet + " by=" + user + " name=" + saved.name() + " grade="
                + saved.grade() + (termsChanged ? " terms=" + (terms == null ? "" : terms) : "")
                + (contactChanged ? " contact=" + (contact == null ? "" : contact) : ""));
        rollup.record(sheet, user, before);
    }
}
