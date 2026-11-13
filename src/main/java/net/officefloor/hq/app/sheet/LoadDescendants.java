package net.officefloor.hq.app.sheet;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class LoadDescendants {
    public void load(@Val Sheet sheet, SheetRepository repository, Out<Descendants> descendants) {
        descendants.set(new Descendants(repository.descendants(sheet.id())));
    }
}
