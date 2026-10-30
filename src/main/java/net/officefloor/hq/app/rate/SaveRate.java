package net.officefloor.hq.app.rate;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class SaveRate {
    public void save(@PathVariable("sheetId") String sheetId, @Val ValidRate rate, Rates rates, Audit audit,
            ObjectResponse<List<RateResponse>> response) {
        rates.set(sheetId, rate.grade(), rate.rate());
        audit.recordOnCommit("RATE_SET sheet=" + sheetId + " by=" + rate.user()
                + " grade=" + rate.grade() + " rate=" + rate.rate().toPlainString());
        response.send(rates.list(sheetId));
    }
}
