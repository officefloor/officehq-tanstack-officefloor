package net.officefloor.hq.app.person;

import java.util.Collection;

/** The person form's rules, shared by the form and the import box. */
public final class PersonRules {

    private PersonRules() {
    }

    /** The person form's rules: the request trimmed and normalised, or refused; the grade must be one of the sheet's grades. */
    public static PersonRequest check(PersonRequest request, Collection<String> grades) throws InvalidPersonException {
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new InvalidPersonException("A name is required");
        }
        String grade = request.grade() == null ? "" : request.grade().trim();
        if (!grades.contains(grade)) {
            throw new InvalidPersonException("Grade must be one of the sheet's grades");
        }
        String gradeFrom = request.gradeFrom() == null ? "" : request.gradeFrom().trim();
        if (!gradeFrom.isEmpty()) {
            try {
                java.time.LocalDate.parse(gradeFrom);
            } catch (java.time.format.DateTimeParseException e) {
                throw new InvalidPersonException("The grade's from-date must be a date");
            }
        }
        String terms = request.terms() == null ? "" : request.terms().trim();
        if (!terms.isEmpty() && !terms.matches("\\d{1,6}")) {
            throw new InvalidPersonException("The terms must be a whole number of days, 0 or more");
        }
        if (!terms.isEmpty()) {
            terms = String.valueOf(Integer.parseInt(terms));
        }
        String termsFrom = request.termsFrom() == null ? "" : request.termsFrom().trim();
        if (!termsFrom.isEmpty()) {
            try {
                java.time.LocalDate.parse(termsFrom);
            } catch (java.time.format.DateTimeParseException e) {
                throw new InvalidPersonException("The terms' from-date must be a date");
            }
        }
        String contact = request.contact() == null ? "" : request.contact().trim().toLowerCase(java.util.Locale.ROOT);
        String tags = net.officefloor.hq.app.claim.Tags.normalise(request.tags());
        return new PersonRequest(name, grade, request.user(), request.version(), gradeFrom, terms, contact, tags == null ? "" : tags, termsFrom);
    }
}
