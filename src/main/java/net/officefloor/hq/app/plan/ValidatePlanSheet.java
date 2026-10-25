package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Refuses a plan sheet that is not one of the sheet's descendants (its children, their children, and so on). A plan
 *  naming no sheet is not checked. */
public class ValidatePlanSheet {
    public void validate(@Val PlanInput input, @Val Sheet sheet, SheetRepository sheets) throws InvalidPlanException {
        if (input.sheet() != null && !sheets.descendantsOf(sheet.getId()).contains(input.sheet())) {
            throw new InvalidPlanException("The sheet " + input.sheet() + " is not a descendant of this sheet.");
        }
    }
}
