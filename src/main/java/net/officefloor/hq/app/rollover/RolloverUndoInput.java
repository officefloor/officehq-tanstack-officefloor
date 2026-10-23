package net.officefloor.hq.app.rollover;

/** A validated rollover undo: who is undoing it. */
public record RolloverUndoInput(String user) {
}
