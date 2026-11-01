package net.officefloor.hq.app.quickadd;

import net.officefloor.hq.app.claim.ClaimRequest;
import net.officefloor.hq.app.claim.ClaimValues;
import net.officefloor.hq.app.grade.Grades;
import net.officefloor.hq.app.person.PersonRequest;
import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Checks the bar by the person and claim forms' rules: a person of the sheet's default grade (A when it is retired) by that name, and a
 *  claim of that amount dated the sheet's entry date (else its today) with an empty note. */
public class ValidateQuickAdd {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody QuickAddRequest request, Settings settings, Grades grades, Out<PersonRequest> person,
            Out<ClaimValues> claim, Out<Actor> actor) throws InvalidSheetRequestException {
        String user = request == null ? null : request.user();
        person.set(PersonRequest.checked(new PersonRequest(request == null ? null : request.name(), grades.orFallback(sheetId, settings.defaultGrade(sheetId)), user, null)));
        claim.set(ClaimRequest.checked(new ClaimRequest(request == null ? null : request.amount(), null, "", null, user, null, null)));
        actor.set(new Actor(user));
    }
}
