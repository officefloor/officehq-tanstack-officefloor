package net.officefloor.hq.app.period;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class AddPeriod {
    public void add(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by,
            @Val ValidPeriod request, SheetRepository sheets, PeriodRepository periods,
            Out<Period> period) throws InvalidPeriodException {
        String id = sheetId.trim();
        String user = by == null ? "" : by;
        if (periods.nameTaken(id, request.name())) {
            throw new InvalidPeriodException("The name is already in use");
        }
        if (periods.overlaps(id, request.from(), request.to())) {
            throw new InvalidPeriodException("The dates overlap another period");
        }
        sheets.open(id, user);
        period.set(periods.add(id, request.name(), request.from(), request.to(), user));
    }
}
