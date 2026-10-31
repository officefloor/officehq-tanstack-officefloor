package net.officefloor.hq.app.period;

import net.officefloor.hq.app.settings.Settings;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Closes the period, fixing its due-total (see {@link PeriodCloser}). A closed period cannot be
 *  closed again. Only the roles the permission matrix allows may close it (clerk and steward by
 *  default). Publishes the closed period. */
public class ClosePeriod {
    public void close(@Val Period period, @Val PeriodCloseRequest request, Settings settings, PeriodCloser closer,
            Out<ClosedPeriod> closed) throws PeriodClosedException {
        if (!settings.allows(period.sheetId(), "close", request.user())) {
            throw new PeriodClosedException("Your role may not close period " + period.name());
        }
        if (period.closed()) throw new PeriodClosedException("Period " + period.name() + " is already closed");
        closed.set(new ClosedPeriod(closer.close(period)));
    }
}
