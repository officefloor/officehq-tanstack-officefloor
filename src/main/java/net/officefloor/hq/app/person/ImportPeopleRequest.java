package net.officefloor.hq.app.person;

/** Body of an import of people: lines of name,grade,terms,contact, and who imports them. */
public record ImportPeopleRequest(String text, String by) {
}
