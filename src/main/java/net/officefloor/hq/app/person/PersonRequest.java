package net.officefloor.hq.app.person;

/**
 * Body of a person add or edit: the name, the grade (A, B or C), the date (yyyy-MM-dd) an edit
 * records the grade from (blank for the sheet's today; an add's grade applies from the beginning of
 * time) and who makes the change. An edit also carries the version of the person its editor loaded
 * (none for an add). {@code terms} are the person's own terms in whole days, blank (null) for none.
 * {@code contact} is their contact, trimmed and lower-cased, blank (null) for none. {@code tags} are their
 * tags, comma separated, stored as a claim's are (see {@link net.officefloor.hq.app.claim.Tags}), blank (null) for none.
 * {@code termsFrom} is the date (yyyy-MM-dd) the terms are recorded from, keeping the ones already recorded;
 * blank (null) records them from the beginning of time.
 */
public record PersonRequest(String name, String grade, String gradeFrom, String user, Long version, String terms, String contact, String tags,
        String termsFrom) {
}
