package net.officefloor.hq.app.person;

/**
 * A person as the screen sends it, to add or to edit: a name, a grade (A, B or C) and the date the
 * grade is from (yyyy-MM-dd; blank for the sheet's today). An edit also names the version of the
 * person it was made from. A contact is any text (blank: none). Their own terms are a whole number of days, 0 or more (blank: none).
 */
public record PersonRequest(String name, String grade, Long version, String gradeFrom, String terms, String contact) {
    /** The terms as a number of days; null when blank. */
    public Integer termsDays() { return terms == null || terms.isEmpty() ? null : Integer.valueOf(terms); }

    /** The contact as stored: trimmed and lower-cased; null when blank (none). */
    public String contactValue() {
        String c = contact == null ? "" : contact.trim().toLowerCase(java.util.Locale.ROOT);
        return c.isEmpty() ? null : c;
    }

    /** A person's name as the form takes it: trimmed, required, at most 200 characters. */
    public static String checkName(String raw) throws InvalidPersonException {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty()) throw new InvalidPersonException("A name is required");
        if (name.length() > 200) throw new InvalidPersonException("A name is at most 200 characters");
        return name;
    }

    /** The request as the person form takes it, checked by the form's rules: trimmed, terms and contact normalised. */
    public static PersonRequest checked(PersonRequest request) throws InvalidPersonException {
        if (request == null) throw new InvalidPersonException("A name is required");
        String name = PersonRequest.checkName(request.name());
        String grade = request.grade() == null ? "" : request.grade().trim();
        if (!java.util.Set.of("A", "B", "C").contains(grade)) throw new InvalidPersonException("A grade is one of A, B or C");
        String gradeFrom = request.gradeFrom() == null ? "" : request.gradeFrom().trim();
        if (!gradeFrom.isEmpty()) {
            try {
                java.time.LocalDate.parse(gradeFrom);
            } catch (java.time.format.DateTimeParseException e) {
                throw new InvalidPersonException("A from-date is a date, yyyy-MM-dd");
            }
        }
        String terms = request.terms() == null ? "" : request.terms().trim();
        if (!terms.isEmpty() && !terms.matches("\\d{1,6}")) {
            throw new InvalidPersonException("Terms are a whole number of days, 0 or more, or blank");
        }
        if (!terms.isEmpty()) terms = String.valueOf(Integer.parseInt(terms));
        String contact = request.contact() == null ? "" : request.contact().trim().toLowerCase(java.util.Locale.ROOT);
        if (contact.length() > 200) throw new InvalidPersonException("A contact is at most 200 characters");
        return new PersonRequest(name, grade, request.version(), gradeFrom, terms, contact);
    }
}
