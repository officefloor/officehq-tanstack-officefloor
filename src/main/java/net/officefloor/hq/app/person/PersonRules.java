package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import net.officefloor.hq.app.sheet.Sheet;

/** The person form's rules, shared by the form and the people import box. */
public final class PersonRules {

    private static final Set<String> GRADES = Set.of("A", "B", "C");

    private static final Pattern TERMS = Pattern.compile("\\d{1,5}");

    private PersonRules() {
    }

    /**
     * Checks the form's fields, trimming the name; refuses an empty name or a grade other than A, B or C, a grade
     * from-date that is not a calendar date, and terms that are not a whole number of days; the contact is trimmed
     * and lower-cased; the tags are normalised as Tags.normalise does.
     */
    /** Checks the terms from-date: trimmed, blank allowed, otherwise a calendar date. */
    public static String checkTermsFrom(String termsFrom) throws InvalidPersonException {
        termsFrom = termsFrom == null ? "" : termsFrom.trim();
        if (!termsFrom.isEmpty()) {
            try {
                LocalDate.parse(termsFrom);
            } catch (DateTimeParseException e) {
                throw new InvalidPersonException("Not a date: " + termsFrom);
            }
        }
        return termsFrom;
    }

    public static PersonRequest check(String name, String grade, Long version, String gradeFrom, String terms,
            String contact, String tags) throws InvalidPersonException {
        name = name == null ? "" : name.trim();
        grade = grade == null ? "" : grade.trim();
        if (name.isEmpty()) {
            throw new InvalidPersonException("A name is required.");
        }
        if (!GRADES.contains(grade)) {
            throw new InvalidPersonException("The grade must be A, B or C.");
        }
        gradeFrom = gradeFrom == null ? "" : gradeFrom.trim();
        if (!gradeFrom.isEmpty()) {
            try {
                LocalDate.parse(gradeFrom);
            } catch (DateTimeParseException e) {
                throw new InvalidPersonException("Not a date: " + gradeFrom);
            }
        }
        terms = terms == null ? "" : terms.trim();
        if (!terms.isEmpty() && !TERMS.matcher(terms).matches()) {
            throw new InvalidPersonException("The terms must be a whole number of days, 0 or more, or blank.");
        }
        contact = contact == null ? "" : contact.trim().toLowerCase(Locale.ROOT);
        return new PersonRequest(name, grade, version, gradeFrom, terms, contact,
                net.officefloor.hq.app.claim.Tags.normalise(tags));
    }

    /**
     * Makes the person to add to the sheet, unsaved: refused when the name is a name or alias on the sheet or on
     * another sheet of its tree, ignoring case, or the contact is another person's on the sheet.
     */
    public static Person make(PersonRequest request, Sheet sheet, PersonRepository repository,
            PersonAliasRepository aliases, TreeNames tree) throws InvalidPersonException {
        if (repository.existsBySheetIdAndNameIgnoreCase(sheet.getId(), request.name())
                || aliases.existsBySheetIdAndAliasIgnoreCase(sheet.getId(), request.name())) {
            throw new InvalidPersonException("The name is already in use on this sheet.");
        }
        if (tree.usedAround(sheet.getId(), request.name())) {
            throw new InvalidPersonException("The name is already in use on a sheet of this sheet's tree.");
        }
        if (request.contactOrNull() != null
                && repository.existsBySheetIdAndContact(sheet.getId(), request.contactOrNull())) {
            throw new InvalidPersonException("The contact is already in use on this sheet.");
        }
        Person person = new Person(sheet.getId(), request.name(), request.grade());
        person.setAddedOn(sheet.getToday());
        if (request.termsFromDate() == null) {
            person.setTerms(request.termsDays());
        } else {
            person.setTermsFrom(request.termsFromDate(), request.termsDays());
        }
        person.setContact(request.contactOrNull());
        person.setTags(request.tags());
        return person;
    }

    /** Refuses a grade retired on the sheet: people on it keep it, but it cannot be chosen for a new person or in an edit. */
    public static void refuseRetired(String sheetId, String grade,
            net.officefloor.hq.app.grade.RetiredGradeRepository retired) throws InvalidPersonException {
        if (net.officefloor.hq.app.grade.RetiredGrades.isRetired(sheetId, grade, retired)) {
            throw new InvalidPersonException("The grade " + grade + " is retired.");
        }
    }

    /** The PERSON_ADDED record for the added person: their terms when they have any, their contact when they have one. */
    public static String addedLine(Person person, String by) {
        return "PERSON_ADDED sheet=" + person.getSheetId() + " by=" + by
                + " name=" + person.getName() + " grade=" + person.getGrade()
                + (person.getTerms() == null ? "" : " terms=" + person.getTerms())
                + (person.getContact() == null ? "" : " contact=" + person.getContact());
    }

    /** The PERSON_EDITED record for the edited person, once saved, with the grade in force on the sheet's today, and
     *  their terms when the edit changed them (empty when cleared), and likewise their contact. */
    public static String editedLine(Person person, String grade, String by) {
        return editedLine(person, grade, person.getTerms(), by);
    }

    /** The PERSON_EDITED record, with their terms in force on the sheet's today when the edit changed any of theirs. */
    public static String editedLine(Person person, String grade, Integer termsToday, String by) {
        return "PERSON_EDITED sheet=" + person.getSheetId() + " by=" + by
                + " name=" + person.getName() + " grade=" + grade
                + (person.isTermsChanged() ? " terms=" + (termsToday == null ? "" : termsToday) : "")
                + (person.isContactChanged() ? " contact=" + (person.getContact() == null ? "" : person.getContact()) : "");
    }
}
