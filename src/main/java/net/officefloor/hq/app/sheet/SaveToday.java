package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.period.PeriodCloser;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class SaveToday {
    public void save(@PathVariable("sheetId") String sheetId, @Val LocalDate today, Sheets sheets,
            @Val TodayRequest request, @Val RollupFigures before, Rollup rollup, PeriodCloser closer, Claims claims,
            ObjectResponse<TodayResponse> response) {
        // Each person's due-total as it stood on the day being left, before the change takes effect.
        rollup.recordSnapshots(sheetId, request.user(), before);
        sheets.setToday(sheetId, today);
        // A change of today releases every open editor's lock.
        claims.unlockAll(sheetId);
        closer.closeDue(sheetId, request.user());
        rollup.recordChanges(sheetId, request.user(), before);
        response.send(TodayResponse.of(sheetId, sheets));
    }
}
