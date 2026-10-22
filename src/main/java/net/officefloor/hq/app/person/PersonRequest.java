package net.officefloor.hq.app.person;

/** Body of a person add or edit: who is making it, and the person's name and grade. An edit also
 *  names the version of the person it was made from (null: not checked), and the date its grade applies
 *  from (yyyy-mm-dd; null: the sheet's today). An add's grade applies from the beginning of time. */
public record PersonRequest(String user, String name, String grade, Long version, String gradeFrom) {
}
