package net.officefloor.hq.app.person;

/** The import box as typed: lines of name,grade,terms,contact, and the signed-in user (`by`) making the change. */
public record ImportPeopleRequest(String lines, String by) {
}
