package net.officefloor.hq.app.setting;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Sets the settings the form sets: a setting that carries a figure (the clerk limit, floor, terms, late percent,
 *  each grade's own terms, cap) is recorded from its from-date (the sheet's today when none is given), replacing a
 *  value recorded from that date and keeping the others; the policy date and period numbering from are set. */
public class ApplySettings {
    public void apply(@Val SettingsInput input, @Val Sheet sheet, SheetRepository repository) {
        for (SettingsInput.Change change : input.changes()) {
            if (change.dated()) {
                sheet.record(change.key(), change.from() != null ? change.from() : sheet.getToday(), change.value());
            } else if ("policy-date".equals(change.key())) {
                sheet.setPolicyDate(change.value() == null ? null : LocalDate.parse(change.value()));
            } else if ("period-numbering-from".equals(change.key())) {
                sheet.setPeriodNumberingFrom(change.value() == null ? null : LocalDate.parse(change.value()));
            }
        }
        repository.saveAndFlush(sheet);
    }
}
