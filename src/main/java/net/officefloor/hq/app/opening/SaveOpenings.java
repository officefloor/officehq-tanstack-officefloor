package net.officefloor.hq.app.opening;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Add each line's amount to its person's carried figure for its period, in order, writing OPENING_SET for each. */
public class SaveOpenings {
    public void save(@PathVariable("sheetId") String sheetId, @Val OpeningImportInput input, @Val Openings checked,
            PeriodRepository periods, Audit audit, Out<OpeningImportResponse> result) {
        String sheet = sheetId.trim();
        for (Opening opening : checked.openings()) {
            periods.addOpening(sheet, opening.period(), opening.personId(), opening.amount());
            audit.record("OPENING_SET sheet=" + sheet + " by=" + input.user() + " name=" + opening.name()
                    + " period=" + opening.period() + " amount=" + opening.amount().toPlainString());
        }
        result.set(new OpeningImportResponse(checked.openings().size()));
    }
}
