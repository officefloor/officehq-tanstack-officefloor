package net.officefloor.hq.app.setting;

import java.time.LocalDate;
import java.util.List;

/** A validated settings form: the settings it sets, in the order they are recorded — the clerk limit, the floor, the
 *  terms, the late percent, the policy date, the period numbering from, the terms of grades A, B and C, the cap, the proof threshold, the
 *  permission matrix entries for open, settle, void, reassign, close and reopen, the grace days, the note threshold, the entry date, each grade's grace days and proof threshold, the pot, the parent sheet. */
public record SettingsInput(List<Change> changes) {

    /** A setting the form sets: its key (as the audit names it), its value as the screen shows it (null: blank —
     *  no clerk limit, no cap, a grade on the sheet's terms, the policy date on the sheet's today, the period
     *  numbering off), and whether it is dated, and the from-date given (null: none given — a dated setting is then
     *  recorded from the sheet's today). */
    public record Change(String key, String value, boolean dated, LocalDate from) {
    }

    public boolean sets(String key) {
        return changes.stream().anyMatch(c -> c.key().equals(key));
    }
}
