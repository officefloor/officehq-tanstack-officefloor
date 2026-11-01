package net.officefloor.hq.app.patron;

/** Body of POST /api/sheets/{sheet}/people/{person}/patrons: the id of the person named as patron. */
public record PatronRequest(Long patron) {
}
