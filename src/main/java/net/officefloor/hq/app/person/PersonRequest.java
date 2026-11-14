package net.officefloor.hq.app.person;

/** Body of an add or edit of a person. An edit carries the version of the person its form loaded. */
public record PersonRequest(String name, String grade, Long version) {
}
