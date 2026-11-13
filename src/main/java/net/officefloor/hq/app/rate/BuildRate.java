package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class BuildRate {
    public void build(@Val RateRequest request, @Val Sheet sheet, Out<Rate> rate) {
        rate.set(new Rate(sheet.id(), request.grade(), new BigDecimal(request.rate())));
    }
}
