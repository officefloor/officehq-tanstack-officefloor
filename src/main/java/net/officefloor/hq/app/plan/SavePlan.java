package net.officefloor.hq.app.plan;

import java.time.LocalDate;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Saves the plan for the person, a blank start being the sheet's today (refused when the end is before it,
 *  or when the sheet it names is not one of the sheet's descendants). */
public class SavePlan {
    public void save(@Val PlanValues values, @Val Person person, Plans plans, Sheets sheets, Settings settings,
            Out<Plan> plan) throws InvalidSheetRequestException {
        LocalDate start = values.start() != null ? values.start() : sheets.today(person.getSheetId());
        if (values.end().isBefore(start)) {
            throw new InvalidSheetRequestException("The end date cannot be before the start");
        }
        if (values.sheet() != null && !settings.descendants(person.getSheetId()).contains(values.sheet())) {
            throw new InvalidSheetRequestException("The sheet " + values.sheet() + " is not a descendant of this sheet");
        }
        long id = plans.add(person.getSheetId(), person.getId(), values.amount(), start, values.every(), values.day(),
                values.end(), values.sheet());
        plan.set(new Plan(id, person.getSheetId(), person.getId(), values.amount(), start, values.every(), values.day(),
                values.end(), 0, java.util.List.of(), java.util.List.of(), values.sheet()));
    }
}
