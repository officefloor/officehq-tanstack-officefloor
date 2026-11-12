package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.hq.app.sheet.InvalidSheetException;

/** How a period is closed, by hand or by a new today past its grace days. */
public final class PeriodClosing {

    private PeriodClosing() {
    }

    /** Closes the open period, fixing its due-total and each person's due-total in it as they are now;
     *  refused when it was closed meanwhile. Shared by a close by hand and the auto close of a new today. */
    public static PeriodResponse fix(String sheet, PeriodResponse period, PeriodStore store, RollupService rollups)
            throws InvalidSheetException {
        LocalDate from = LocalDate.parse(period.from());
        LocalDate to = LocalDate.parse(period.to());
        Map<Long, BigDecimal> people = rollups.dueBetweenByPerson(sheet, from, to);
        BigDecimal total = people.values().stream().reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        if (!store.close(sheet, period.name(), total)) {
            throw new InvalidSheetException("The period " + period.name() + " is already closed");
        }
        store.fixPersonTotals(sheet, period.name(), people);
        return PeriodResponse.of(period.name(), from, to, true, total, people);
    }
}
