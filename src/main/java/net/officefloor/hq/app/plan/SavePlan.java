package net.officefloor.hq.app.plan;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Adds the plan to the person, audited as PLAN_ADDED. A blank start is the sheet's today; an end before the start is refused. */
public class SavePlan {
    public void save(@Val ValidPlan request, @Val Person person, Plans plans, Sheets sheets, Audit audit,
            ObjectResponse<ResponseEntity<PlanResponse>> response) throws InvalidPlanException {
        String sheetId = person.getSheetId();
        LocalDate start = request.start() != null ? request.start() : sheets.today(sheetId);
        if (request.end().isBefore(start)) {
            throw new InvalidPlanException("A plan's end cannot be before its start");
        }
        Plan plan = plans.add(sheetId, person.getId(), request.amount(), start, request.every(), request.day(), request.end(), request.target());
        audit.recordOnCommit("PLAN_ADDED sheet=" + sheetId + " by=" + request.user() + " name=" + person.getName()
                + " amount=" + plan.amount().toPlainString() + " start=" + plan.start() + (plan.day() != null ? " day=" + plan.day() : " every=" + plan.every()) + " end=" + plan.end()
                + (plan.target() != null ? " sheet=" + plan.target() : ""));
        response.send(new ResponseEntity<>(PlanResponse.from(plan, sheets.today(sheetId)), HttpStatus.CREATED));
    }
}
