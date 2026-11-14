package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class SetRate {
    public void set(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by,
            @Val ValidRate request, SheetRepository sheets, RateRepository rates, Out<Rate> rate) {
        String id = sheetId.trim();
        String user = by == null ? "" : by;
        sheets.open(id, user);
        rate.set(rates.set(id, request.grade(), request.pct(), user));
    }
}
