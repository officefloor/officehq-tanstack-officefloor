package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Refuse a plan naming a sheet that is not one of its sheet's descendants. */
public class CheckPlanSheet {
    public void check(@PathVariable("sheetId") String sheetId, @Val PlanInput input, SheetRepository sheets)
            throws InvalidPlanException {
        if (input.sheet() != null && !sheets.descendants(sheetId.trim()).contains(input.sheet())) {
            throw new InvalidPlanException("The sheet must be one of this sheet's descendants.");
        }
    }
}
