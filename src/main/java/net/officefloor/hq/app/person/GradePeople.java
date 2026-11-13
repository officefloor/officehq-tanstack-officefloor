package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.grade.GradeStore;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Gives each selected person the grade from the from-date, in list order (by their positions),
 *  exactly as the person form's edit would (keeping their name and terms), each recorded as PERSON_EDITED
 *  and then how it moved the sheet's figures. A selected person not on the sheet refuses the whole change; a viewer grades no one (0, nothing stored). */
public class GradePeople {
    public void grade(@PathVariable("sheetId") String sheetId, @Val BulkGradeInput input, PersonStore store,
            SheetStore sheets, RoleStore roles, RollupService rollups, Audit audit, GradeStore grades,
            Out<BulkGradeResult> result)
            throws InvalidSheetException, StalePersonException {
        String sheet = sheetId.trim();
        List<Person> selected = new ArrayList<>();
        for (Long id : input.personIds()) {
            Person person = store.find(sheet, id);
            if (person == null) {
                throw new InvalidSheetException("No such person: " + id);
            }
            selected.add(person);
        }
        if (roles.find(sheet, input.user()) == Role.VIEWER) {
            result.set(new BulkGradeResult(0));
            return;
        }
        List<Long> order = store.list(sheet).stream().map(Person::getId).toList();
        selected.sort(Comparator.comparingInt(p -> order.indexOf(p.getId())));
        LocalDate today = sheets.today(sheet);
        for (Person person : selected) {
            grades.refuseChoice(sheet, input.grade(), person, input.gradeFrom(), today);
        }
        for (Person person : selected) {
            Rollup before = rollups.snapshot(sheet);
            PersonRules.applyGrade(person, input.grade(), input.gradeFrom(), today);
            if (!store.save(person)) {
                throw new StalePersonException();
            }
            audit.record("PERSON_EDITED sheet=" + sheet + " by=" + input.user() + " name=" + person.getName()
                    + " grade=" + person.gradeOn(today));
            rollups.record(sheet, input.user(), before);
        }
        result.set(new BulkGradeResult(selected.size()));
    }
}
