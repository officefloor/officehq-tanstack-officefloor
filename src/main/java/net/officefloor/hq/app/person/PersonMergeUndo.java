package net.officefloor.hq.app.person;

/** An undo of a merge once checked: the person merged into, the merge undone, the name of the person it restores, and who
 *  undoes it. */
public record PersonMergeUndo(Person into, MergeUndoer.LastMerge merge, String name, String by) {
}
