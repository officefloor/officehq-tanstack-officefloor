package net.officefloor.hq.app.person;

/** The person form: the request body to add or edit a person. An edit carries the {@code version}
 *  of the person its form loaded (null when adding). */
public record PersonRequest(String name, String grade, Long version) {
}
