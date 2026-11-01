package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** A grade's rate on a sheet: a percent with two decimals, in force from `from` (null: from the beginning of time)
 *  through `to` (null: with no end). `version` counts the values set for its grade and from-date, this
 *  newest one included (0 for one not yet saved). `forwardAfter` is set for a forward-only rate, one saved
 *  with a from-date on or after the sheet's today: it applies only to claims whose sequence is higher (those
 *  added after it was saved); null for one that applies to every claim. `earlier` are its grade and
 *  from-date's older versions, newest first: a claim a forward-only version does not apply to takes the
 *  newest of them that does. */
public record Rate(String sheetId, String grade, BigDecimal pct, LocalDate from, LocalDate to, int version,
        Long forwardAfter, List<Version> earlier) {
    public Rate(String sheetId, String grade, BigDecimal pct, LocalDate from, LocalDate to) {
        this(sheetId, grade, pct, from, to, 0, null, List.of());
    }

    /** An older version of a rate: its percent, to-date and forward-only mark as it was set. */
    public record Version(BigDecimal pct, LocalDate to, Long forwardAfter) {
    }
}
