package net.officefloor.hq.app.person;

/** The signed-in user (`by`) undoing the transfer that brought a person here. */
public record TransferUndoRequest(String by) {
}
