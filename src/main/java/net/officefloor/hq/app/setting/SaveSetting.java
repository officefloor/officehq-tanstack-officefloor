package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Saves the setting, a dated one from its from-date (none given: the sheet's today); a floor moves the live dues, so
 * the figures it moved are audited after it.
 */
public class SaveSetting {
    public void save(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val SettingInput input,
            Settings settings, SheetClock clock, Audit audit, Rollup rollup) {
        Rollup.Snapshot before = rollup.take(sheet);
        settings.set(sheet, input.key(), input.from() == null ? clock.today(sheet) : input.from(), input.value());
        audit.record("SETTING_CHANGED sheet=" + sheet + " by=" + user + " key=" + input.key() + " value="
                + input.value() + (input.from() == null ? "" : " from=" + input.from()));
        rollup.record(sheet, user, before);
    }
}
