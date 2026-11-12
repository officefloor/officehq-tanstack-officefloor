package net.officefloor.hq.app.opening;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Adds each pasted amount in order to the person's opening balance for the period, recorded as
 *  OPENING_SET sheet=<id> by=<user> name=<name> period=<period> amount=<amount>. */
public class SaveOpeningImport {
    public void save(@PathVariable("sheetId") String sheetId, @Val OpeningImportInput imported, OpeningStore store,
            Audit audit, Out<OpeningImportResult> result) {
        String sheet = sheetId.trim();
        for (OpeningLine line : imported.lines()) {
            store.add(sheet, line.period(), line.person().getId(), line.amount());
            audit.record("OPENING_SET sheet=" + sheet + " by=" + imported.user() + " name=" + line.person().getName()
                    + " period=" + line.period() + " amount=" + line.amount().setScale(2).toPlainString());
        }
        result.set(new OpeningImportResult(imported.lines().size()));
    }
}
