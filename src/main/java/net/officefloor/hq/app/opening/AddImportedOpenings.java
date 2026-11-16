package net.officefloor.hq.app.opening;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Keeps the checked lines in the order pasted, each adding its amount to the person's carried figure for the period,
 *  written as OPENING_SET. */
public class AddImportedOpenings {
    public void add(@PathVariable("sheetId") String sheetId, @Val ImportedOpenings imported, OpeningRepository openings,
            Audit audit, Out<ImportOpeningsResult> result) {
        String sheet = sheetId.trim();
        for (OpeningLine line : imported.lines()) {
            openings.add(sheet, line.period().id(), line.person().id(), line.amount());
            audit.record("OPENING_SET sheet=" + sheet + " by=" + imported.by() + " name=" + line.person().name()
                    + " period=" + line.period().name() + " amount=" + line.amount().toPlainString());
        }
        result.set(new ImportOpeningsResult(imported.lines().size()));
    }
}
