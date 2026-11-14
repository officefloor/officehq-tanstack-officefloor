package net.officefloor.hq.app.period;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class BuildPeriod {
    public void build(@Val PeriodRequest request, @Val Sheet sheet, Out<Period> period) {
        period.set(new Period(sheet.id(), request.name(), LocalDate.parse(request.from()), LocalDate.parse(request.to()),
                request.cap().isEmpty() ? null : Integer.valueOf(request.cap())));
    }
}
