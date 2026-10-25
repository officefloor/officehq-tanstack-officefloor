package net.officefloor.hq.app.setting;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.SettingLog;
import net.officefloor.hq.app.sheet.SettingLogRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Sets the settings the form sets: a setting that carries a figure (the clerk limit, floor, terms, late percent,
 *  each grade's own terms, cap, proof threshold, note threshold, each grade's grace days, each grade's proof threshold, review threshold) is recorded from its from-date (the sheet's today when none is given), replacing a
 *  value recorded from that date and keeping the others; the policy date, period numbering from and entry date are set; a permission matrix entry
 *  (key perm-<action>), the grace days, the pot, the parent sheet, the fiscal year start month and the alert threshold and the escalation gap and the maximum of open claims and the review SLA and the pending SLA and the proof kinds and the default grade and the score thresholds are recorded undated. Each is logged with the sheet's today (so the
 *  settings can be worked out as they stood on an earlier date). */
public class ApplySettings {
    public void apply(@Val SettingsInput input, @Val Sheet sheet, SheetRepository repository, SettingLogRepository log) {
        for (SettingsInput.Change change : input.changes()) {
            LocalDate from = null;
            if (change.dated()) {
                from = change.from() != null ? change.from() : sheet.getToday();
                sheet.record(change.key(), from, change.value());
            } else if (change.key().startsWith("perm-") || Sheet.GRACE_DAYS.equals(change.key())
                    || Sheet.ESCALATE_AFTER.equals(change.key()) || Sheet.MAX_OPEN.equals(change.key()) || Sheet.REVIEW_SLA.equals(change.key())
                    || Sheet.PENDING_SLA.equals(change.key())
                    || Sheet.PROOF_KINDS.equals(change.key()) || Sheet.DEFAULT_GRADE.equals(change.key())
                    || Sheet.POT.equals(change.key()) || Sheet.PARENT.equals(change.key())
                    || Sheet.FISCAL_START_MONTH.equals(change.key())
                    || Sheet.ALERT_THRESHOLD.equals(change.key())
                    || Sheet.SCORE_MID.equals(change.key()) || Sheet.SCORE_HIGH.equals(change.key())
                    || Sheet.SCORE_LOG.equals(change.key())) {
                sheet.record(change.key(), null, change.value());
            } else if (Sheet.POLICY_DATE.equals(change.key())) {
                sheet.setPolicyDate(change.value() == null ? null : LocalDate.parse(change.value()));
            } else if (Sheet.PERIOD_NUMBERING_FROM.equals(change.key())) {
                sheet.setPeriodNumberingFrom(change.value() == null ? null : LocalDate.parse(change.value()));
            } else if (Sheet.ENTRY_DATE.equals(change.key())) {
                sheet.setEntryDate(change.value() == null ? null : LocalDate.parse(change.value()));
            } else {
                continue;
            }
            log.save(new SettingLog(sheet.getId(), change.key(), from, change.value(), sheet.getToday()));
        }
        repository.saveAndFlush(sheet);
        log.flush();
    }
}
