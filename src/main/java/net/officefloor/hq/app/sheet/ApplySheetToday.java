package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import net.officefloor.hq.app.asof.AsOfStore;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class ApplySheetToday {
    public void apply(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by,
            @Val LocalDate today, SheetRepository repository, ClaimRepository claims, AsOfStore asOf, Out<Sheet> sheet) {
        String id = sheetId.trim();
        Sheet was = repository.find(id);
        if (was != null && !today.equals(LocalDate.parse(was.today()))) {
            // what the sheet holds now is what it holds as of every date up to its old today
            asOf.snapshot(id, LocalDate.parse(was.today()), today);
        }
        repository.open(id, by == null ? "" : by);
        if (!today.equals(repository.today(id))) {
            claims.unlockAll(id); // a new today closes every open claim editor
        }
        repository.setToday(id, today);
        sheet.set(repository.find(id));
    }
}
