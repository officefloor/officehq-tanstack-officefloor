package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.grade.Grades;
import net.officefloor.hq.app.claim.ClaimRequest;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;

/** Body of a person add or edit: the name, the grade (A, B, C or one added to the sheet), who makes the change and, for an
 *  edit, the version of the person the editor loaded (null: no check), and the date the grade is
 *  in force from (yyyy-mm-dd; blank or null for the sheet's today), and the person's own terms in days
 *  (blank or null for none), and the person's contact (blank or null for none), and their tags, comma
 *  separated (blank for none; null keeps the tags they have), and the date the terms are in force from
 *  (yyyy-mm-dd; blank or null: from the beginning of time). */
public record PersonRequest(String name, String grade, String user, Integer version, String gradeFrom, String terms,
        String contact, String tags, String termsFrom) {

    public PersonRequest(String name, String grade, String user, Integer version, String gradeFrom, String terms,
            String contact, String tags) {
        this(name, grade, user, version, gradeFrom, terms, contact, tags, null);
    }

    public PersonRequest(String name, String grade, String user, Integer version, String gradeFrom, String terms,
            String contact) {
        this(name, grade, user, version, gradeFrom, terms, contact, null);
    }

    public PersonRequest(String name, String grade, String user, Integer version) {
        this(name, grade, user, version, null, null, null, null);
    }

    /** The contact given, without the spaces around it and lower-cased, or null when it was left blank. */
    public String contactKey() {
        return contact == null || contact.isBlank() ? null : contact.trim().toLowerCase(java.util.Locale.ROOT);
    }

    /** The tags given as stored (see {@link ClaimRequest#tags}), or null when none were given. */
    public String tagsKey() {
        return ClaimRequest.tags(tags);
    }

    /** The terms given, or null when they were left blank. */
    public Integer termsDays() {
        return terms == null || terms.isBlank() ? null : Integer.valueOf(terms.trim());
    }

    /** The from-date given, or null when it was left blank. */
    public LocalDate gradeFromDate() {
        return gradeFrom == null || gradeFrom.isBlank() ? null : LocalDate.parse(gradeFrom.trim());
    }

    /** The terms' from-date given, or null when it was left blank. */
    public LocalDate termsFromDate() {
        return termsFrom == null || termsFrom.isBlank() ? null : LocalDate.parse(termsFrom.trim());
    }

    /** The request as the person rules accept it, or refused. */
    public static PersonRequest checked(PersonRequest request) throws InvalidSheetRequestException {
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new InvalidSheetRequestException("A name is required");
        }
        // Whether it is one of the sheet's grades is checked against the sheet (see Grades#refuseRetired).
        if (request.grade() == null || !Grades.CODE.matcher(request.grade()).matches()) {
            throw new InvalidSheetRequestException("The grade must be one to three capital letters");
        }
        String from = date(request.gradeFrom());
        String termsFrom = date(request.termsFrom());
        String terms = request.terms() == null ? "" : request.terms().trim();
        if (!terms.isEmpty() && !terms.matches("\\d{1,4}")) {
            throw new InvalidSheetRequestException("Terms must be a whole number of days: " + terms);
        }
        // Names are kept without the spaces around them.
        return new PersonRequest(request.name().trim(), request.grade(), request.user(), request.version(), from, terms,
                request.contactKey(), request.tags(), termsFrom);
    }

    /** The date given, trimmed (blank: none), or refused when it is not one. */
    private static String date(String given) throws InvalidSheetRequestException {
        String date = given == null ? "" : given.trim();
        if (!date.isEmpty()) {
            try {
                LocalDate.parse(date);
            } catch (DateTimeParseException e) {
                throw new InvalidSheetRequestException("Not a date (yyyy-mm-dd): " + date);
            }
        }
        return date;
    }
}
