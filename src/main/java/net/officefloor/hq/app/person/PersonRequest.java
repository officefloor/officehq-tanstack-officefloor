package net.officefloor.hq.app.person;

/** Body of an add or edit of a person, and who makes it. An edit carries the version of the person it was made from,
 *  and the date (yyyy-MM-dd) its grade is from: blank for the sheet's today. {@code terms} are the person's own terms in
 *  days, blank for none. {@code contact}
 *  is the person's contact, blank for none. */
public record PersonRequest(String name, String grade, String by, Long version, String gradeFrom, String terms, String contact) {
}
