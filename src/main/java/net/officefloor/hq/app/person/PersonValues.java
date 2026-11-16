package net.officefloor.hq.app.person;

/** A person's values once validated, and who gives them; for an edit, the version it was made from (null for an add). */
public record PersonValues(String name, String grade, String by, Long version) {
}
