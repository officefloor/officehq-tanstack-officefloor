package net.officefloor.hq.app.sheet;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class EnsureSheet {
    public void ensure(@Val SheetRequest request, SheetRepository repository, Out<Sheet> sheet) {
        sheet.set(repository.ensure(request.id()));
    }
}
