package net.officefloor.hq.app.opening;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Adds each line's amount to its person's opening balance for its period, in order, recorded as OPENING_SET. */
public class SaveOpenings {
    public void save(@Val FoundOpenings found, @RequestParam("user") String user, OpeningRepository repository,
            Audit audit, Out<ImportOpeningsResult> result) {
        int added = 0;
        for (PastedOpening line : found.lines()) {
            repository.add(line.person().sheetId(), line.period().name(), line.person().id(), line.amount());
            audit.record("OPENING_SET sheet=" + line.person().sheetId() + " by=" + user + " name="
                    + line.person().name() + " period=" + line.period().name() + " amount="
                    + line.amount().toPlainString());
            added++;
        }
        result.set(new ImportOpeningsResult(added));
    }
}
