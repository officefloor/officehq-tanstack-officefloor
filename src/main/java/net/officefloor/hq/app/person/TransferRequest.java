package net.officefloor.hq.app.person;

/** Body of POST /api/sheets/{sheet}/people/{person}/transfer: the id of the sheet they are transferred to. */
public record TransferRequest(String to) {
}
