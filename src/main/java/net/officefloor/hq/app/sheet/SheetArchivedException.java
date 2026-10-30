package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.setting.ViewerRefusedException;

/** A change asked for on an archived sheet: everything stays visible and nothing changes, so it is refused and nothing is written. */
public class SheetArchivedException extends ViewerRefusedException {
    public SheetArchivedException() {
        super("The sheet is archived: nothing on it can change");
    }
}
