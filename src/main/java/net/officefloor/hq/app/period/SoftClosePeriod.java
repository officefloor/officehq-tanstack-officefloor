package net.officefloor.hq.app.period;

import net.officefloor.hq.app.settings.Settings;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Soft-closes the period, fixing its due-total and each person's (see {@link PeriodCloser}) while
 *  claims dated in it can still be changed. Only an open period can be soft-closed, and only by the
 *  roles the permission matrix allows to close it. Publishes the soft-closed period. */
public class SoftClosePeriod {
    public void close(@Val Period period, @Val PeriodCloseRequest request, Settings settings, PeriodCloser closer,
            Out<ClosedPeriod> closed) throws PeriodClosedException {
        if (!settings.allows(period.sheetId(), "close", request.user())) {
            throw new PeriodClosedException("Your role may not close period " + period.name());
        }
        if (period.closed()) throw new PeriodClosedException("Period " + period.name() + " is already closed");
        closed.set(new ClosedPeriod(closer.close(period, true)));
    }
}
