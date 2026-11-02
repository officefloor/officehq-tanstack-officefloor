package net.officefloor.hq.app.sheet;

/** Body of POST /api/sheets/{sheet}/clone (and its response): the id of the sheet cloned to. */
public record CloneRequest(String to) {
}
