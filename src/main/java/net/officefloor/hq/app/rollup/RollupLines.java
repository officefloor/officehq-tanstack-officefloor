package net.officefloor.hq.app.rollup;

import java.math.BigDecimal;
import net.officefloor.hq.app.Audit;

/** The ROLLUP, TOTALS and ALERT lines a change records, shared by the steps that record them. */
public final class RollupLines {

    private RollupLines() {
    }

    /** Record the lines for what moved since {@code before}; the sheet's figures now, for a later comparison. */
    public static Figures record(String sheet, String user, Figures before, Rollup rollup, Audit audit) {
        Figures after = rollup.figures(sheet);
        after.people().forEach((id, now) -> {
            if (!now.sameAs(before.people().get(id))) {
                audit.record(Audit.line("ROLLUP", sheet, user, "person=" + now.name(), "claimed=" + now.claimed(),
                        "due-total=" + now.dueTotal(), "overdue-total=" + now.overdueTotal()));
            }
        });
        if (!after.claimedTotal().equals(before.claimedTotal()) || !after.dueTotal().equals(before.dueTotal())
                || !after.overdueTotal().equals(before.overdueTotal())) {
            audit.record(Audit.line("TOTALS", sheet, user, "claimed-total=" + after.claimedTotal(),
                    "due-total=" + after.dueTotal(), "overdue-total=" + after.overdueTotal()));
        }
        // Then one ALERT per person whose due-total went from at or below the alert threshold (or none) to above it, in list order.
        BigDecimal threshold = rollup.alertThreshold(sheet);
        if (threshold != null) {
            after.people().forEach((id, now) -> {
                Figures.PersonFigures was = before.people().get(id);
                if (above(now, threshold) && !above(was, threshold)) {
                    audit.record(Audit.line("ALERT", sheet, user, "name=" + now.name(), "due-total=" + now.dueTotal()));
                }
            });
        }
        return after;
    }

    /** Whether the person has a due-total above the threshold. */
    private static boolean above(Figures.PersonFigures figures, BigDecimal threshold) {
        return figures != null && !"n/a".equals(figures.dueTotal()) && new BigDecimal(figures.dueTotal()).compareTo(threshold) > 0;
    }
}
