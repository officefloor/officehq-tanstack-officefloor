package net.officefloor.hq.app.person;

/**
 * Body of a person add or edit: the name, the grade (A, B or C), the date (yyyy-MM-dd) an edit
 * records the grade from (blank for the sheet's today; an add's grade applies from the beginning of
 * time) and who makes the change. An edit also carries the version of the person its editor loaded
 * (none for an add). {@code terms} are the person's own terms in whole days, blank (null) for none.
 * {@code contact} is their contact, trimmed and lower-cased, blank (null) for none.
 */
public record PersonRequest(String name, String grade, String gradeFrom, String user, Long version, String terms, String contact) {
}
