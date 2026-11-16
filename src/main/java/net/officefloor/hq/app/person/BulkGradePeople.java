package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.grade.GradeRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetArchivedException;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Gives the selected people the grade in list order (by their places in the list), each as the
 *  person form would (see {@link ApplyPerson}: from the from-date, blank the sheet's today) and
 *  recorded as it would record it (its PERSON_EDITED line, then the rollup lines it moved). A
 *  selected id not on the sheet is skipped. A viewer may grade no one: none are given it. Refused on an
 *  archived sheet. */
public class BulkGradePeople {
    public void grade(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by, @Val BulkGradeRequest request,
            PersonRepository people, ClaimRepository claims, RateRepository rates,
            SheetRepository sheets, SettingRepository settings, RoleRepository roles, GradeRepository grades, Audit audit, Out<BulkGradeResult> result)
            throws PersonConflictException, SheetArchivedException, InvalidPersonException {
        String id = sheetId.trim();
        String user = by == null ? "" : by;
        if (sheets.isArchived(id)) {
            throw new SheetArchivedException("The sheet is archived");
        }
        if (Role.VIEWER.equals(roles.roleOf(id, user))) {
            result.set(new BulkGradeResult(0));
            return;
        }
        Set<Long> selected = new HashSet<>(request.personIds());
        // A retired grade cannot be given: refused before anyone is graded.
        if (grades.isRetired(id, request.grade())) {
            for (Person listed : people.list(id)) {
                if (selected.contains(listed.getId())
                        && (request.from() != null || !request.grade().equals(listed.getGrade()))) {
                    throw new InvalidPersonException("The grade is retired: " + request.grade());
                }
            }
        }
        LocalDate today = sheets.today(id);
        int graded = 0;
        Rollup before = Rollup.of(id, people, claims, rates, sheets, settings);
        for (Person listed : people.list(id)) {
            if (!selected.contains(listed.getId())) continue;
            Person person = people.find(id, listed.getId());
            // As the form: no from-date and the grade already in force gives none.
            if (request.from() != null || !request.grade().equals(person.getGrade())) {
                person.giveGrade(request.grade(), request.from() != null ? request.from() : today.toString(), today);
            }
            if (!people.update(person)) {
                throw new PersonConflictException(person.getId());
            }
            if (person.getRecorded() != null) {
                people.saveGrade(person, person.getRecorded(), user);
            }
            graded++;
            audit.record("PERSON_EDITED sheet=" + id + " by=" + user + " name=" + person.getName()
                    + " grade=" + person.getGrade());
            Rollup after = Rollup.of(id, people, claims, rates, sheets, settings);
            for (String line : after.changesSince(before, id, user)) {
                audit.record(line);
            }
            before = after;
        }
        result.set(new BulkGradeResult(graded));
    }
}
