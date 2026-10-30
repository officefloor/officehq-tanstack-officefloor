package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import net.officefloor.hq.app.period.PeriodCloser;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class SaveToday {
    public void save(@PathVariable("sheetId") String sheetId, @Val LocalDate today, Sheets sheets,
            @Val TodayRequest request, @Val RollupFigures before, Rollup rollup, PeriodCloser closer,
            ObjectResponse<TodayResponse> response) {
        sheets.setToday(sheetId, today);
        closer.closeDue(sheetId, request.user());
        rollup.recordChanges(sheetId, request.user(), before);
        response.send(new TodayResponse(sheetId, today.toString()));
    }
}
