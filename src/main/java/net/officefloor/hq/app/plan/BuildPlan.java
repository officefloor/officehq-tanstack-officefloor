package net.officefloor.hq.app.plan;

import java.time.LocalDate;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Adds the plan to its person (start blank: the sheet's today), refusing an end before that start first. */
public class BuildPlan {
    public void build(@Val Person person, @Val PlanInput input, SheetRepository sheets, PlanRepository repository,
            Out<Plan> plan) throws BadRequestException {
        LocalDate start = input.start() != null ? input.start() : sheets.today(person.getSheetId());
        if (input.end().isBefore(start)) {
            throw new BadRequestException(ValidatePlan.END_BEFORE_START);
        }
        plan.set(repository.insert(person.getSheetId(), person.getId(), input.amount(), start, input.every(),
                input.day(), input.end()));
    }
}
