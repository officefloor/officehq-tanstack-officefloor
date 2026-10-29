package net.officefloor.hq.app.person;

/**
 * The body of an add or an edit of a person: the name as typed and the grade (A, B or C). An edit
 * also names the {@code version} its form loaded (null on an add), and {@code gradeFrom}, the date
 * (yyyy-mm-dd) the grade is given from, blank for the sheet's today. An add's grade is the first one,
 * which applies from the beginning of time, so it ignores {@code gradeFrom}. {@code terms} are the person's own
 * terms, a whole number of days, null (blank) for none; an edit records them from {@code termsFrom} (yyyy-mm-dd) on,
 * keeping those recorded from other dates, or, when it is null (blank), from the beginning of time. An add's terms apply from
 * the beginning of time, so it ignores {@code termsFrom}. {@code contact} is the person's contact, trimmed and
 * lower-cased, null (blank) for none.
 * {@code tags} are the person's tags, comma separated: each trimmed and lower-cased, blanks and repeats dropped, sorted
 * (null for none).
 */
public record PersonRequest(String name, String grade, Long version, String gradeFrom, String terms, String contact, String tags, String termsFrom) {
}
