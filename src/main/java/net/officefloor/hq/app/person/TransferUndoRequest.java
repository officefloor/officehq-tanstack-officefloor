package net.officefloor.hq.app.person;

/** Body of undoing the transfer that brought a person to this sheet: who is doing it. */
public record TransferUndoRequest(String user) {
}
