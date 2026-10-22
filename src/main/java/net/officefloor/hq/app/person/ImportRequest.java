package net.officefloor.hq.app.person;

/** Body of a people import: who is making it and the pasted text, one name,grade,terms,contact line per person. */
public record ImportRequest(String user, String text) {
}
