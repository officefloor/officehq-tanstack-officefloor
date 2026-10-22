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

/** Retire each selected person from the date (the sheet's today when none was given), in list order (the order
 *  they were added), exactly as the row's retire would: its PERSON_RETIRED line and then how the sheet's figures
 *  moved over it. A selected id not on the sheet is skipped. A viewer's bulk retire retires nobody and writes
 *  nothing. */
public class BulkRetirePeople {
    public void retire(@PathVariable("sheetId") String sheetId, @Val BulkRetireInput input, PersonRepository people,
            SheetRepository sheets, RoleRepository roles, Rollups rollups, Audit audit, Out<BulkRetireResponse> result) {
        String sheet = sheetId.trim();
        if (Role.VIEWER.equals(roles.of(sheet, input.user()))) {
            result.set(new BulkRetireResponse(0));
            return;
        }
        LocalDate from = input.from() != null ? input.from() : sheets.today(sheet);
        int retired = 0;
        for (Person loaded : people.list(sheet)) {
            if (!input.personIds().contains(loaded.id())) {
                continue;
            }
            Rollup before = rollups.take(sheet);
            people.retire(loaded, from);
            audit.record("PERSON_RETIRED sheet=" + sheet + " by=" + input.user()
                    + " name=" + loaded.name() + " from=" + from);
            rollups.record(sheet, input.user(), before);
            retired++;
        }
        result.set(new BulkRetireResponse(retired));
    }
}
