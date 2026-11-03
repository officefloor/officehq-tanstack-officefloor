package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

/** Add a plan to a person on the sheet, written as PLAN_ADDED; a plan naming a sheet that is not a descendant of
 *  the sheet is refused. */
public class SavePlan {
    public void save(@PathVariable("sheetId") String sheetId, @PathVariable("personId") String personId,
            @Val PlanInput input, ServerHttpConnection connection, PlanRepository repository, Audit audit)
            throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        long pid;
        try {
            pid = Long.parseLong(personId == null ? "" : personId.trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException("no such person");
        }
        String name = repository.personName(id, pid);
        if (name == null) {
            throw new BadRequestException("no such person");
        }
        if (input.sheet() != null && !repository.descendant(input.sheet(), id)) {
            throw new BadRequestException(input.sheet() + " is not a descendant of this sheet");
        }
        Plan saved = repository.add(id, pid, input);
        audit.record("PLAN_ADDED sheet=" + id + " by=" + by + " name=" + name + " amount=" + saved.amount()
                + " start=" + saved.start()
                + (saved.day() != null ? " day=" + saved.day() : " every=" + saved.every()) + " end=" + saved.end()
                + (saved.sheet() != null ? " sheet=" + saved.sheet() : ""));
    }
}
