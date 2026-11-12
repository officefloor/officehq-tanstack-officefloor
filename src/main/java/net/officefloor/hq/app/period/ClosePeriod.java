package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

/** Closes the period, fixing its due-total as it is now: the figures of the claims that count whose
 *  date falls in it, skipping n/a. Refused when there is no such period or it is already closed. */
public class ClosePeriod {
    public void close(@PathVariable("sheetId") String sheetId, @PathVariable("periodName") String periodName,
            PeriodStore store, RollupService rollups, Out<PeriodResponse> closed) throws InvalidSheetException {
        String sheet = sheetId.trim();
        PeriodResponse period = store.find(sheet, periodName.trim());
        if (period == null) {
            throw new InvalidSheetException("No such period: " + periodName.trim());
        }
        if (period.closed()) {
            throw new InvalidSheetException("The period " + period.name() + " is already closed");
        }
        LocalDate from = LocalDate.parse(period.from());
        LocalDate to = LocalDate.parse(period.to());
        BigDecimal total = rollups.dueBetween(sheet, from, to);
        if (!store.close(sheet, period.name(), total)) {
            throw new InvalidSheetException("The period " + period.name() + " is already closed");
        }
        closed.set(PeriodResponse.of(period.name(), from, to, true, total));
    }
}
