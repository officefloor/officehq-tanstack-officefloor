package net.officefloor.hq.app.person;

/** Body of an import: lines of name,grade,terms,contact (terms and contact may be absent or blank). */
public record ImportRequest(String text) {
}
