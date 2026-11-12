package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;

/** The person form's rules, shared by the form and the people import. */
public final class PersonRules {
    private static final Set<String> GRADES = Set.of("A", "B", "C");

    private PersonRules() {
    }

    /** The person form's rules for a name, grade, from-date, terms, contact and tags: the request as stored
     *  (name trimmed, blanks as none, contact lower-cased, tags as {@link net.officefloor.hq.app.claim.Tags} stores them), or refused. */
    public static PersonRequest check(PersonRequest request) throws InvalidSheetException {
        if (request == null || request.name() == null || request.name().trim().isEmpty()) {
            throw new InvalidSheetException("A name is required");
        }
        if (request.grade() == null || !GRADES.contains(request.grade())) {
            throw new InvalidSheetException("The grade must be A, B or C");
        }
        String from = request.gradeFrom() == null ? "" : request.gradeFrom().trim();
        try {
            if (!from.isEmpty()) {
                LocalDate.parse(from);
            }
        } catch (DateTimeParseException e) {
            throw new InvalidSheetException("Not a date: " + from);
        }
        String terms = request.terms() == null ? "" : request.terms().trim();
        if (!terms.isEmpty() && !terms.matches("\\d{1,6}")) {
            throw new InvalidSheetException("The terms must be a whole number of days, 0 or more, or blank");
        }
        String contact = request.contact() == null ? "" : request.contact().trim().toLowerCase(java.util.Locale.ROOT);
        // Stored and compared without the spaces around the name; a blank from-date as none.
        return new PersonRequest(request.name().trim(), request.grade(), from.isEmpty() ? null : from,
                request.user(), request.version(),
                terms.isEmpty() ? null : String.valueOf(Integer.parseInt(terms)), contact.isEmpty() ? null : contact,
                net.officefloor.hq.app.claim.Tags.normalise(request.tags()));
    }

    /** Refuses a name another person on the sheet, or anyone on another sheet of its tree, has as a name or
     *  alias, or a contact another person on the sheet has. */
    public static void refuseTaken(String sheetId, PersonRequest request, PersonStore store, SettingStore settings)
            throws DuplicateNameException, DuplicateContactException {
        if (store.nameTaken(sheetId.trim(), request.name(), null)
                || store.usedOn(settings.tree(sheetId.trim()), request.name())) {
            throw new DuplicateNameException(request.name());
        }
        if (request.contact() != null && store.contactTaken(sheetId.trim(), request.contact(), null)) {
            throw new DuplicateContactException(request.contact());
        }
    }

    /** Records the grade from the from-date (yyyy-MM-dd, null for the sheet's today), as an edit does: nothing
     *  when the from-date is null and the grade is the one in force on the sheet's today. */
    public static void applyGrade(Person person, String grade, String gradeFrom, LocalDate today) {
        if (gradeFrom == null && grade.equals(person.gradeOn(today))) {
            return;
        }
        person.recordGrade(gradeFrom == null ? today : LocalDate.parse(gradeFrom), grade);
    }
}
