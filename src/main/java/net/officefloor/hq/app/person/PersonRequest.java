package net.officefloor.hq.app.person;

/** Body of POST /api/sheets/{sheet}/people and PUT /api/sheets/{sheet}/people/{person}. */
public record PersonRequest(String name, String grade) {
}
