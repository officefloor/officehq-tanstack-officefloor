package net.officefloor.hq.app.voidreason;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class LoadVoidReasons {
    public void load(@Val Sheet sheet, VoidReasonRepository repository, Out<VoidReasons> reasons) {
        reasons.set(new VoidReasons(repository.findAll(sheet.id())));
    }
}
