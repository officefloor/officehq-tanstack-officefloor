package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

public class ApplyPerson {
    public void apply(@Val PersonRequest request, @Val Person person, PersonRepository people,
            SheetRepository sheets)
            throws InvalidPersonException, PersonConflictException {
        if (request.version() != null && request.version() != person.getVersion()) {
            throw new PersonConflictException(person.getId());
        }
        if (people.nameTaken(person.getSheetId(), request.name(), person.getId())) {
            throw new InvalidPersonException("The name is already in use");
        }
        // Someone clashing with another sheet of the tree can only be saved under a name, and with
        // aliases, that no longer clash.
        java.util.Set<String> tree = people.treeNames(person.getSheetId());
        if (tree.contains(request.name().trim().toLowerCase(java.util.Locale.ROOT))) {
            throw new InvalidPersonException("The name is already in use on the sheet's tree");
        }
        if (person.getAliases().stream().anyMatch((a) -> tree.contains(a.trim().toLowerCase(java.util.Locale.ROOT)))) {
            throw new InvalidPersonException("An alias is already in use on the sheet's tree");
        }
        if (request.contact() != null && people.contactTaken(person.getSheetId(), request.contact(), person.getId())) {
            throw new InvalidPersonException("The contact is already in use");
        }
        person.setName(request.name());
        person.giveContact(request.contact());
        person.setTags(request.tags());
        // Terms with a from-date are given from it, keeping the others; with none, from the beginning of time.
        Integer terms = request.terms() == null ? null : Integer.valueOf(request.terms());
        if (request.termsFrom() != null) {
            person.giveTermsFrom(terms, request.termsFrom());
        } else {
            person.giveTerms(terms);
        }
        // The form's grade is given from its from-date (blank: the sheet's today); a save leaving
        // the grade as the row showed it, with no from-date, gives none.
        if (request.gradeFrom() != null || !request.grade().equals(person.getGrade())) {
            java.time.LocalDate today = sheets.today(person.getSheetId());
            person.giveGrade(request.grade(), request.gradeFrom() != null ? request.gradeFrom() : today.toString(), today);
        }
    }
}
