package net.officefloor.hq.app.person;

/**
 * People pasted into the import box as the screen sends them: one person per line, as
 * {@code name,grade,terms,contact}. The terms and contact may be absent or blank.
 */
public record ImportPeopleRequest(String text) {
}
