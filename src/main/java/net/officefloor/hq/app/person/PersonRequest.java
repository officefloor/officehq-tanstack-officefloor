package net.officefloor.hq.app.person;

/** Body of an add or edit of a person, and who makes it. An edit carries the version of the person it was made from,
 *  and the date (yyyy-MM-dd) its grade is from: blank for the sheet's today. {@code terms} are the person's own terms in
 *  days, blank for none. {@code contact}
 *  is the person's contact, blank for none. {@code tags} are the person's tags, comma separated (null or blank: none).
 *  {@code termsFrom} is the date (yyyy-MM-dd) the terms are from: blank (or null) for the beginning of time. */
public record PersonRequest(String name, String grade, String by, Long version, String gradeFrom, String terms, String contact,
        String tags, String termsFrom) {

    public PersonRequest(String name, String grade, String by, Long version, String gradeFrom, String terms, String contact,
            String tags) {
        this(name, grade, by, version, gradeFrom, terms, contact, tags, null);
    }
}
