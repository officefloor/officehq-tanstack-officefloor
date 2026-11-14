package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class BuildRate {
    public void build(@Val RateRequest request, @Val Sheet sheet, Out<Rate> rate) {
        LocalDate from = request.from() == null ? null : LocalDate.parse(request.from());
        // From the sheet's today on: forward-only, applying only to claims added after it is saved.
        boolean forward = from != null && !from.isBefore(sheet.today());
        rate.set(new Rate(sheet.id(), request.grade(), new BigDecimal(request.rate()), from,
                request.to() == null ? null : LocalDate.parse(request.to()), 1, forward, 0, null));
    }
}
