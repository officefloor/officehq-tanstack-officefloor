package net.officefloor.hq.app.peopleimport;

/** Body of an import of people, as typed: lines of name,grade,terms,contact and who makes the change. */
public record ImportRequest(String text, String user) {
}
