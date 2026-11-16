package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class ApplyToday {
    public void apply(@Val Sheet sheet, @Val LocalDate today, Out<Sheet> changed) {
        changed.set(new Sheet(sheet.id(), today, sheet.archived(), sheet.parent()));
    }
}
