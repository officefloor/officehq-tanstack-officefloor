package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Undoes the merge (see PersonMergeStore#undo), publishing the person restored. */
public class UndoMerge {
    public void undo(@Val PersonMergeStore.Merge merge, @Val Person person, PersonMergeStore merges,
            PersonStore people, Out<MergeUndone> undone) {
        merges.undo(person.getSheetId(), merge);
        undone.set(new MergeUndone(people.find(person.getSheetId(), merge.fromId())));
    }
}
