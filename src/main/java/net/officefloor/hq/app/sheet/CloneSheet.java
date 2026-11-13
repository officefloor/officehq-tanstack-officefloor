package net.officefloor.hq.app.sheet;

import net.officefloor.plugin.variable.Val;

/** Makes the clone (see {@link SheetCloner}). */
public class CloneSheet {
    public void cloneSheet(@Val SheetClone clone, SheetCloner cloner) {
        cloner.cloneSheet(clone.from(), clone.to());
    }
}
