package net.officefloor.hq.app.rollup;

import net.officefloor.hq.app.Audit;

/** The ROLLUP and TOTALS lines a change records, shared by the steps that record them. */
public final class RollupLines {

    private RollupLines() {
    }

    /** Record the lines for what moved since {@code before}; the sheet's figures now, for a later comparison. */
    public static Figures record(String sheet, String user, Figures before, Rollup rollup, Audit audit) {
        Figures after = rollup.figures(sheet);
        after.people().forEach((id, now) -> {
            if (!now.sameAs(before.people().get(id))) {
                audit.record(Audit.line("ROLLUP", sheet, user, "person=" + now.name(), "claimed=" + now.claimed(),
                        "due-total=" + now.dueTotal()));
            }
        });
        if (!after.claimedTotal().equals(before.claimedTotal()) || !after.dueTotal().equals(before.dueTotal())) {
            audit.record(Audit.line("TOTALS", sheet, user, "claimed-total=" + after.claimedTotal(),
                    "due-total=" + after.dueTotal()));
        }
        return after;
    }
}
