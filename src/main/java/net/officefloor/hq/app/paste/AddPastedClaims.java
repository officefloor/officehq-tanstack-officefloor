package net.officefloor.hq.app.paste;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.claim.ClaimLog;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Adds a draft claim per checked line, in order, as the claim form's BuildClaim adds one. */
public class AddPastedClaims {
    public void add(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @Val PastedClaims pasted, Claims claims, Settings settings, ClaimLog log, Audit audit, Rollup rollup,
            ObjectResponse<PasteResponse> response) {
        for (PastedClaim line : pasted.lines()) {
            Person person = line.person();
            ClaimInput input = line.input();
            Rollup.Snapshot before = rollup.take(sheet);
            long id = claims.add(sheet, person.id(), input, input.date().plusDays(
                    settings.terms(sheet, person.gradeOn(input.date()), input.date())));
            log.record(sheet, user, id);
            audit.record("CLAIM_ADDED sheet=" + sheet + " by=" + user + " " + input.auditFields(person.name()));
            rollup.record(sheet, user, before);
        }
        response.send(new PasteResponse(pasted.lines().size(), claims.list(sheet)));
    }
}
