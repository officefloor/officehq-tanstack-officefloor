package net.officefloor.hq.app.person;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * A person's fields as the form submits them: a name (required), a grade (A, B or C) and the date the grade
 * is in force from (yyyy-mm-dd; blank is the sheet's today, and is kept blank here) and the person's own terms
 * (a whole number of days, 0 or more; blank: none, kept blank here). An edit also sends the version of the person
 * its editor loaded; an add sends none. The contact is kept trimmed and lower-cased (blank: none, kept blank here).
 * The tags, comma separated, are kept lower-cased, sorted, without repeats (blank: none, kept blank here; not given:
 * an edit keeps the person's tags). The terms' from-date (yyyy-mm-dd) records the terms from that date; blank (kept
 * blank here) records them from the beginning of time.
 */
public record PersonRequest(@Size(max = 200) String name, @Size(max = 10) String grade,
        @Size(max = 30) String gradeFrom, Integer version, @Size(max = 30) String terms,
        @Size(max = 200) String contact, @Size(max = 1000) String tags, @Size(max = 30) String termsFrom) {

    /** A request with no terms from-date. */
    public PersonRequest(String name, String grade, String gradeFrom, Integer version, String terms, String contact,
            String tags) {
        this(name, grade, gradeFrom, version, terms, contact, tags, "");
    }

    /** The terms' from-date, or null when blank; call only once validated. */
    public LocalDate termsFromDate() {
        return termsFrom == null || termsFrom.isEmpty() ? null : LocalDate.parse(termsFrom);
    }

    /** The terms as a number of days, or null when blank; call only once validated. */
    public Integer termsDays() {
        return terms == null || terms.isEmpty() ? null : Integer.valueOf(terms);
    }

    /** The contact, or null when blank; call only once validated. */
    public String contactOrNull() {
        return contact == null || contact.isEmpty() ? null : contact;
    }

    /** The tags in their stored form, or null when blank; call only once validated. */
    public String tagsOrNull() {
        return tags == null || tags.isEmpty() ? null : tags;
    }
}
