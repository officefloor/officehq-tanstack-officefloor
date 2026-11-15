package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Before a new today takes effect, while the score log is on: one SCORE line per person, in list
 *  order, with the score as it stood on the day being left; nothing when the today does not change. */
public class RecordScores {
    public void record(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by, @Val LocalDate today,
            @Val Rollup before, SheetRepository sheets, SettingRepository settings, Audit audit) {
        String id = sheetId.trim();
        if (today.equals(sheets.today(id)) || !settings.scoreLog(id)) return;
        String head = "SCORE sheet=" + id + " by=" + (by == null ? "" : by);
        before.people().values().forEach((figures) ->
                audit.record(head + " name=" + figures.name() + " score=" + figures.score()));
    }
}
