package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Adds each checked line's amount to the person's opening balance for the period, in order, recorded as
 *  OPENING_SET. A closed or soft-closed period keeps the carried figure fixed when it was closed. */
public class ImportOpenings {
    public void importOpenings(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by, @Val OpeningLines lines,
            SheetRepository sheets, PeriodRepository periods, Audit audit, Out<ImportOpeningsResult> result) {
        String id = sheetId.trim();
        String user = by == null ? "" : by;
        sheets.open(id, user);
        for (OpeningLines.Line line : lines.lines()) {
            periods.addOpening(id, line.period(), line.personId(), line.amount());
            audit.record("OPENING_SET sheet=" + id + " by=" + user + " name=" + line.name() + " period=" + line.period()
                    + " amount=" + line.amount().toPlainString());
        }
        result.set(new ImportOpeningsResult(lines.lines().size()));
    }
}
