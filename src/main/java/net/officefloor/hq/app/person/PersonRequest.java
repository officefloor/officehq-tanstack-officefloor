package net.officefloor.hq.app.person;

/** The person form; {@code version} is the version the editor loaded (an edit only). */
public record PersonRequest(String name, String grade, Long version) {
}
