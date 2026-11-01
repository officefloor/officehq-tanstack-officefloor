package net.officefloor.hq.app.person;

/** Body of undoing the last merge into a person: who makes the change. */
public record MergeUndoRequest(String user) {
}
