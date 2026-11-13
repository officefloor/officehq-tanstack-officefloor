package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class ApplySheetToday {
    public void apply(@Val LocalDate today, @Val Sheet sheet, Out<Sheet> updated) {
        updated.set(new Sheet(sheet.id(), today));
    }
}
