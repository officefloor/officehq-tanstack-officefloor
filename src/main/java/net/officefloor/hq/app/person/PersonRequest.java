package net.officefloor.hq.app.person;

/** Body of an add or edit of a person, and who makes it. An edit carries the version of the person it was made from. */
public record PersonRequest(String name, String grade, String by, Long version) {
}
