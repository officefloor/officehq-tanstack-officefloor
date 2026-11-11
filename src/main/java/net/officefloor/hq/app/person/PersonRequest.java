package net.officefloor.hq.app.person;

/**
 * A person as the screen sends it, to add or to edit: a name, a grade (A, B or C) and the date the
 * grade is from (yyyy-MM-dd; blank for the sheet's today). An edit also names the version of the
 * person it was made from. A contact is any text (blank: none). Tags are comma separated (blank: none). Their own terms are a whole number of days, 0 or more (blank: none), from the terms from-date (yyyy-MM-dd; blank: from the beginning of time).
 */
public record PersonRequest(String name, String grade, Long version, String gradeFrom, String terms, String contact, String tags, String termsFrom) {
    /** The terms as a number of days; null when blank. */
    public Integer termsDays() { return terms == null || terms.isEmpty() ? null : Integer.valueOf(terms); }

    /** The contact as stored: trimmed and lower-cased; null when blank (none). */
    public String contactValue() {
        String c = contact == null ? "" : contact.trim().toLowerCase(java.util.Locale.ROOT);
        return c.isEmpty() ? null : c;
    }

    /** The tags as stored: each trimmed and lower-cased, repeats dropped, sorted and comma separated; null when none. */
    public String tagsValue() {
        java.util.TreeSet<String> set = new java.util.TreeSet<>();
        if (tags != null) {
            for (String tag : tags.split(",")) {
                String t = tag.trim().toLowerCase(java.util.Locale.ROOT);
                if (!t.isEmpty()) set.add(t);
            }
        }
        return set.isEmpty() ? null : String.join(",", set);
    }

    /** A person's name as the form takes it: trimmed, required, at most 200 characters. */
    public static String checkName(String raw) throws InvalidPersonException {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty()) throw new InvalidPersonException("A name is required");
        if (name.length() > 200) throw new InvalidPersonException("A name is at most 200 characters");
        return name;
    }

    /** A from-date as the form takes it: trimmed, blank or a date (yyyy-MM-dd). */
    private static String checkDate(String raw) throws InvalidPersonException {
        String date = raw == null ? "" : raw.trim();
        if (!date.isEmpty()) {
            try {
                java.time.LocalDate.parse(date);
            } catch (java.time.format.DateTimeParseException e) {
                throw new InvalidPersonException("A from-date is a date, yyyy-MM-dd");
            }
        }
        return date;
    }

    /** The request as the person form takes it, checked by the form's rules: trimmed, terms, contact and tags normalised. */
    public static PersonRequest checked(PersonRequest request) throws InvalidPersonException {
        if (request == null) throw new InvalidPersonException("A name is required");
        String name = PersonRequest.checkName(request.name());
        String grade = request.grade() == null ? "" : request.grade().trim();
        if (!net.officefloor.hq.app.setting.Settings.isGradeCode(grade)) throw new InvalidPersonException("A grade is one to three capital letters");
        String gradeFrom = checkDate(request.gradeFrom());
        String terms = request.terms() == null ? "" : request.terms().trim();
        if (!terms.isEmpty() && !terms.matches("\\d{1,6}")) {
            throw new InvalidPersonException("Terms are a whole number of days, 0 or more, or blank");
        }
        if (!terms.isEmpty()) terms = String.valueOf(Integer.parseInt(terms));
        String termsFrom = checkDate(request.termsFrom());
        String contact = request.contact() == null ? "" : request.contact().trim().toLowerCase(java.util.Locale.ROOT);
        if (contact.length() > 200) throw new InvalidPersonException("A contact is at most 200 characters");
        String tags = request.tags() == null ? "" : request.tags();
        PersonRequest checked = new PersonRequest(name, grade, request.version(), gradeFrom, terms, contact, tags, termsFrom);
        String stored = checked.tagsValue();
        if (stored != null && stored.length() > 2000) throw new InvalidPersonException("Tags are at most 2000 characters");
        return checked;
    }
}
