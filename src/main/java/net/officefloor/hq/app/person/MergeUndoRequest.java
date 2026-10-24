package net.officefloor.hq.app.person;

/** Body of undoing the last merge into a person: who is doing it. */
public record MergeUndoRequest(String user) {
}
