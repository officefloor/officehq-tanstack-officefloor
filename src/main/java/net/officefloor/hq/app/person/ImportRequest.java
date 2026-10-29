package net.officefloor.hq.app.person;

/** The body of an import of people, as typed: lines of name,grade,terms,contact (the last two may be absent or blank). */
public record ImportRequest(String text) {
}
