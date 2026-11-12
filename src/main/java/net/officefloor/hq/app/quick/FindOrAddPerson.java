package net.officefloor.hq.app.quick;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRequest;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.person.Retirement;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * The person on the sheet with the quick add's name or alias (ignoring case); when there is none, adds them
 * as the person form would, recording PERSON_ADDED and how it moved the figures. Runs only once the
 * claim has passed every check, so a refused quick add leaves no person behind. Refuses the claim when
 * the person found is retired from a date before it.
 */
public class FindOrAddPerson {
    public void find(@PathVariable("sheetId") String sheetId, @Val PersonRequest request, @Val ClaimInput claim,
            PersonStore store, Audit audit, RollupService rollups, Out<Person> personOut) throws InvalidSheetException {
        String sheet = sheetId.trim();
        Person person = store.findByName(sheet, request.name());
        if (person != null) {
            Retirement.refuseClaimDate(person, claim.date());
        } else {
            Rollup before = rollups.snapshot(sheet);
            person = store.add(sheet, request.name(), request.grade());
            audit.record("PERSON_ADDED sheet=" + sheet + " by=" + request.user() + " name=" + person.getName()
                    + " grade=" + request.grade());
            rollups.record(sheet, request.user(), before);
        }
        personOut.set(person);
    }
}
