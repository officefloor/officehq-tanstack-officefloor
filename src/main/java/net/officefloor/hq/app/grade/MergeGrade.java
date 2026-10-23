package net.officefloor.hq.app.grade;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.PersonResponse;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.Rollups;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Merge a grade into another: only a steward may, and not into a retired grade. GRADE_MERGED is recorded, then
 *  every person whose grade in force on the sheet's today is the merged grade is given the other from the sheet's
 *  today, in list order, exactly as the person form's edit would: a new version, the grade recorded from today
 *  keeping those from other dates, its PERSON_EDITED line and then how the sheet's figures moved over it; then how the sheet's figures moved over the whole merge. */
public class MergeGrade {
    public void merge(@PathVariable("sheetId") String sheetId, @Val GradeMergeRequest request, PeriodRepository periods,
            RoleRepository roles, GradeRepository grades, PersonRepository people, SheetRepository sheets,
            Rollups rollups, Audit audit, Out<GradeMergeResponse> result)
            throws InvalidGradeException, RetiredGradeException {
        String sheet = sheetId.trim();
        periods.lockSheet(sheet);
        if (!Role.STEWARD.equals(roles.of(sheet, request.user()))) {
            throw new InvalidGradeException("Only a steward may merge grades.");
        }
        grades.refuseRetired(sheet, request.into());
        audit.record("GRADE_MERGED sheet=" + sheet + " by=" + request.user()
                + " from=" + request.from() + " into=" + request.into());
        LocalDate today = sheets.today(sheet);
        Rollup start = rollups.take(sheet);
        int changed = 0;
        for (Person loaded : people.list(sheet)) {
            if (!request.from().equals(loaded.grade())) {
                continue;
            }
            Rollup before = rollups.take(sheet);
            Person person = new Person(loaded.id(), loaded.sheetId(), loaded.name(), loaded.grade(),
                    loaded.version() + 1, loaded.statementCount(), loaded.statedTotal(), loaded.statementDate(),
                    loaded.retiredFrom(), loaded.grades(), loaded.holds(), loaded.terms(), loaded.contact(),
                    loaded.aliases(), loaded.origin(), loaded.retirements(), loaded.tags(), loaded.statements());
            if (!people.update(person, loaded.version())) {
                continue;
            }
            people.grade(person, request.into(), today);
            PersonResponse saved = PersonResponse.from(people.find(sheet, loaded.id()));
            audit.record("PERSON_EDITED sheet=" + sheet + " by=" + request.user()
                    + " name=" + saved.name() + " grade=" + saved.grade());
            rollups.record(sheet, request.user(), before);
            changed++;
        }
        rollups.record(sheet, request.user(), start);
        result.set(new GradeMergeResponse(request.from(), request.into(), changed));
    }
}
