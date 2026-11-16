package net.officefloor.hq.app.grade;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonConflictException;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.pool.PoolRepository;
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

/** Merges a grade into another: recorded as GRADE_MERGED, then everyone whose grade in force on the
 *  sheet's today is the merged grade is given the other from the sheet's today, in list order, each
 *  as the person form would (its PERSON_EDITED line, then the rollup lines it moved); then the rollup
 *  lines of what moved over the whole merge, once every grade record of theirs on the merged grade
 *  has become the other (retroactive). Refused, with
 *  nothing written, for anyone but a steward, on an archived sheet, or into a retired grade when
 *  anyone would be given it. */
public class MergeGrade {
    public void merge(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by, @Val MergeGradeRequest request,
            PersonRepository people, ClaimRepository claims, RateRepository rates,
            SheetRepository sheets, SettingRepository settings, PoolRepository pools,
            RoleRepository roles, GradeRepository grades, Audit audit, Out<GradeMerged> result)
            throws InvalidGradeException, SheetArchivedException, PersonConflictException {
        String id = sheetId.trim();
        String user = by == null ? "" : by;
        if (!Role.STEWARD.equals(roles.roleOf(id, user))) {
            throw new InvalidGradeException("Only a steward can merge a grade");
        }
        if (sheets.isArchived(id)) {
            throw new SheetArchivedException("The sheet is archived");
        }
        java.util.List<Person> merged = people.list(id).stream()
                .filter((p) -> request.from().equals(p.getGrade())).toList();
        if (!merged.isEmpty()) {
            try {
                grades.requireChoosable(id, request.into());
            } catch (InvalidPersonException e) {
                throw new InvalidGradeException(e.getMessage());
            }
        }
        audit.record("GRADE_MERGED sheet=" + id + " by=" + user + " from=" + request.from()
                + " into=" + request.into());
        LocalDate today = sheets.today(id);
        Rollup start = Rollup.of(id, people, claims, rates, sheets, settings, pools);
        Rollup before = start;
        int count = 0;
        for (Person listed : merged) {
            Person person = people.find(id, listed.getId());
            person.giveGrade(request.into(), today.toString(), today);
            if (!people.update(person)) {
                throw new PersonConflictException(person.getId());
            }
            if (person.getRecorded() != null) {
                people.saveGrade(person, person.getRecorded(), user);
            }
            count++;
            audit.record("PERSON_EDITED sheet=" + id + " by=" + user + " name=" + person.getName()
                    + " grade=" + person.getGrade());
            Rollup after = Rollup.of(id, people, claims, rates, sheets, settings, pools);
            for (String line : after.changesSince(before, id, user)) {
                audit.record(line);
            }
            before = after;
        }
        // The merge is retroactive: every grade record of theirs on the merged grade becomes the
        // other, so all their claims, whatever their date, use the other grade.
        for (Person listed : merged) {
            people.regrade(people.find(id, listed.getId()), request.from(), request.into(), user);
        }
        before = Rollup.of(id, people, claims, rates, sheets, settings, pools);
        // Then the merge's own rollup lines: what moved over the whole merge.
        for (String line : before.changesSince(start, id, user)) {
            audit.record(line);
        }
        result.set(new GradeMerged(count));
    }
}
