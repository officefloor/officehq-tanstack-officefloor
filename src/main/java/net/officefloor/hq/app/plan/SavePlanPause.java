package net.officefloor.hq.app.plan;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Pause the loaded plan from the sheet's today. Refused when the plan is already paused, or the sheet has no today. */
public class SavePlanPause {
    public void save(@Val PlanPauseRequest input, @Val Plan loaded, SheetRepository sheets, PlanRepository repository,
            Out<PlanResponse> saved) throws InvalidPlanException {
        LocalDate today = sheets.today(loaded.sheetId());
        if (today == null) {
            throw new InvalidPlanException("Set the sheet's today before pausing a plan.");
        }
        if (!repository.pause(loaded.id(), today, input.user())) {
            throw new InvalidPlanException("The plan is already paused.");
        }
        saved.set(PlanResponse.from(repository.find(loaded.sheetId(), loaded.id()), today));
    }
}
