package net.officefloor.hq.app.person;

/** Body of an add or edit of a person. An edit carries the version of the person its form loaded,
 *  and the date its grade applies from (blank: the sheet's today; an add's grade applies from the
 *  beginning of time). */
public record PersonRequest(String name, String grade, String gradeFrom, Long version) {
}
