package net.officefloor.hq.app.person;

/** Body of a people import: lines of name,grade,terms,contact as typed, and who adds them. */
public record ImportPeopleRequest(String text, String user) {
}
