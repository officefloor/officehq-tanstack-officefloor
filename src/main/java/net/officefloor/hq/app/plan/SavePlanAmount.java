package net.officefloor.hq.app.plan;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Change the loaded plan's amount from the sheet's today. Refused when the sheet has no today. */
public class SavePlanAmount {
    public void save(@Val PlanAmountInput input, @Val Plan loaded, SheetRepository sheets, PlanRepository repository,
            Out<PlanResponse> saved) throws InvalidPlanException {
        LocalDate today = sheets.today(loaded.sheetId());
        if (today == null) {
            throw new InvalidPlanException("Set the sheet's today before changing a plan.");
        }
        repository.changeAmount(loaded.id(), today, input.amount(), input.user());
        saved.set(PlanResponse.from(repository.find(loaded.sheetId(), loaded.id()), today));
    }
}
