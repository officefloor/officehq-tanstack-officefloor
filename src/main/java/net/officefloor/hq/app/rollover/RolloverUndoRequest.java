package net.officefloor.hq.app.rollover;

/** Body of a rollover undo: who is undoing it. */
public record RolloverUndoRequest(String user) {
}
