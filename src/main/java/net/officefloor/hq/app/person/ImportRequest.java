package net.officefloor.hq.app.person;

/** Body of POST /api/sheets/{sheet}/people/import: lines of name,grade,terms,contact (the last two may be absent or blank). */
public record ImportRequest(String text) {
}
