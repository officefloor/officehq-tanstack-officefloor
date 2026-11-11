package net.officefloor.hq.app.plan;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Saves the plan for the person, a blank start taken as the sheet's today (its end not before it). A
 * sheet it names must be one of the sheet's descendants.
 */
public class SavePlan {
    public void save(@Val PlanInput input, @Val Sheet sheet, @Val Person person, PlanRepository repository,
            SheetRepository sheets, Out<Plan> planOut) throws InvalidPlanException {
        LocalDate start = input.start() == null ? sheet.today() : input.start();
        if (input.end().isBefore(start)) throw new InvalidPlanException("A plan's end is not before its start");
        if (input.sheet() != null && !sheets.descendants(sheet.id()).contains(input.sheet())) {
            throw new InvalidPlanException("A plan's sheet is one of this sheet's descendants");
        }
        planOut.set(repository.save(new Plan(null, sheet.id(), person.getId(), input.amount(), start, input.every(),
                input.day(), input.end(), 0, List.of(), List.of(), input.sheet())));
    }
}
