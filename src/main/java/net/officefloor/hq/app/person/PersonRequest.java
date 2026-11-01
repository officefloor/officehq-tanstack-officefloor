package net.officefloor.hq.app.person;

/**
 * Body of POST /api/sheets/{sheet}/people and PUT /api/sheets/{sheet}/people/{person}. On a PUT, the
 * version is the one the editor loaded (null to save regardless).
 */
public record PersonRequest(String name, String grade, Long version) {
}
