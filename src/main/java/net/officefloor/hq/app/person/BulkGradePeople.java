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

/** Give each selected person the grade from the from-date (the sheet's today when none was given), in list order
 *  (the order they were added), exactly as the person form's edit would: a new version, the grade recorded from
 *  the date keeping those from other dates, name and terms kept, its PERSON_EDITED line and then how the sheet's
 *  figures moved over it. A selected id not on the sheet is skipped. A viewer's bulk grade changes nobody and writes
 *  nothing. */
public class BulkGradePeople {
    public void grade(@PathVariable("sheetId") String sheetId, @Val BulkGradeInput input, PersonRepository people,
            SheetRepository sheets, RoleRepository roles, Rollups rollups, Audit audit, Out<BulkGradeResponse> result) {
        String sheet = sheetId.trim();
        if (Role.VIEWER.equals(roles.of(sheet, input.user()))) {
            result.set(new BulkGradeResponse(0));
            return;
        }
        LocalDate from = input.from() != null ? input.from() : sheets.today(sheet);
        int changed = 0;
        for (Person loaded : people.list(sheet)) {
            if (!input.personIds().contains(loaded.id())) {
                continue;
            }
            Rollup before = rollups.take(sheet);
            Person person = new Person(loaded.id(), loaded.sheetId(), loaded.name(), loaded.grade(),
                    loaded.version() + 1, loaded.statementCount(), loaded.statedTotal(), loaded.statementDate(),
                    loaded.retiredFrom(), loaded.grades(), loaded.holds(), loaded.terms(), loaded.contact(), loaded.aliases());
            if (!people.update(person, loaded.version())) {
                continue;
            }
            people.grade(person, input.grade(), from);
            PersonResponse saved = PersonResponse.from(people.find(sheet, loaded.id()));
            audit.record("PERSON_EDITED sheet=" + sheet + " by=" + input.user()
                    + " name=" + saved.name() + " grade=" + saved.grade());
            rollups.record(sheet, input.user(), before);
            changed++;
        }
        result.set(new BulkGradeResponse(changed));
    }
}
