package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.Rollups;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Hold each selected person from the from-date (the sheet's today when none was given) to the to-date (none:
 *  open-ended), in list order (the order they were added), exactly as the row's hold would: the hold added to those
 *  they already have, its PERSON_HELD line and then how the sheet's figures moved over it. A selected id not on the
 *  sheet is skipped. Refused, before anything is written, when the to-date is before the sheet's today taken as the
 *  from-date. A viewer's bulk hold holds nobody and writes nothing. */
public class BulkHoldPeople {
    public void hold(@PathVariable("sheetId") String sheetId, @Val BulkHoldInput input, PersonRepository people,
            SheetRepository sheets, RoleRepository roles, Rollups rollups, Audit audit, Out<BulkHoldResponse> result)
            throws InvalidPersonException {
        String sheet = sheetId.trim();
        if (Role.VIEWER.equals(roles.of(sheet, input.user()))) {
            result.set(new BulkHoldResponse(0));
            return;
        }
        LocalDate from = input.from() != null ? input.from() : sheets.today(sheet);
        if (input.to() != null && input.to().isBefore(from)) {
            throw new InvalidPersonException("The hold's to-date must not be before its from-date.");
        }
        int held = 0;
        for (Person loaded : people.list(sheet)) {
            if (!input.personIds().contains(loaded.id())) {
                continue;
            }
            Rollup before = rollups.take(sheet);
            people.hold(loaded, from, input.to());
            audit.record("PERSON_HELD sheet=" + sheet + " by=" + input.user() + " name=" + loaded.name()
                    + " from=" + from);
            rollups.record(sheet, input.user(), before);
            held++;
        }
        result.set(new BulkHoldResponse(held));
    }
}
