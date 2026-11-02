package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Adds each checked line's amount, in order, to its person's opening balance for its period. */
public class SetImportedOpenings {
    public void set(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @Val ImportedOpenings imported, Periods periods, Audit audit,
            ObjectResponse<OpeningImportResponse> response) {
        for (ImportedOpenings.Line line : imported.lines()) {
            periods.addOpening(sheet, line.period(), line.person(), line.amount());
            audit.record("OPENING_SET sheet=" + sheet + " by=" + user + " name=" + line.name() + " period="
                    + line.period() + " amount=" + line.amount().toPlainString());
        }
        response.send(new OpeningImportResponse(imported.lines().size(), periods.list(sheet)));
    }
}
