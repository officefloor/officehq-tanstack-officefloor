package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.Audit;

/** Setting one valid rate, as the rate form and the rate import both do. */
final class SetRate {
    private SetRate() {
    }

    /** Set the rate, replacing one for the same grade and from-date, and audit it as RATE_SET (on commit). */
    static void apply(String sheetId, ValidRate rate, Rates rates, Audit audit) {
        rates.set(sheetId, rate.grade(), rate.from(), rate.to(), rate.rate());
        audit.recordOnCommit("RATE_SET sheet=" + sheetId + " by=" + rate.user()
                + " grade=" + rate.grade() + " rate=" + rate.rate().toPlainString()
                + (rate.from() == null ? "" : " from=" + rate.from())
                + (rate.to() == null ? "" : " to=" + rate.to()));
    }
}
