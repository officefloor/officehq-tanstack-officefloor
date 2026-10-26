package net.officefloor.hq.app.opening;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Adds each pasted amount to the person's opening balance for the period, in order, writing OPENING_SET for each. */
public class SetImportedOpenings {
    public void set(@Val SheetResponse sheet, @Val ImportedOpenings imported, @Val ChangedBy by,
            PeriodRepository periods, Audit audit, Out<ImportOpeningsResponse> response) {
        for (Opening opening : imported.openings()) {
            periods.addOpening(sheet.id(), opening.period().id(), opening.person().getId(), opening.amount());
            audit.record("OPENING_SET sheet=" + sheet.id() + " by=" + by.name() + " name=" + opening.person().getName()
                    + " period=" + opening.period().name() + " amount=" + opening.amount().toPlainString());
        }
        response.set(new ImportOpeningsResponse(imported.openings().size()));
    }
}
