package net.officefloor.hq.app.grade;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.history.AsOf;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonGrade;
import net.officefloor.hq.app.person.PersonGradeRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.PersonRules;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Merges a grade into another: only a steward may, and not into a retired grade. Records GRADE_MERGED, then gives
 *  every person (in list order) whose grade in force on the sheet's today is the merged grade the other from the
 *  sheet's today, each exactly as the person form would save it, with its PERSON_EDITED and rollup lines; then every
 *  grade of theirs on the merged grade, whatever its date (their first grade too), becomes the other. */
public class ApplyGradeMerge {
    public void apply(@Val GradeMergeRequest request, @Val Sheet sheet,
            @RequestHeader(name = "X-User", required = false) String userHeader, RoleRepository roles,
            RetiredGradeRepository retired, AddedGradeRepository added, SheetRepository sheets, PersonRepository people, ClaimRepository claims,
            RateRepository rates, PersonGradeRepository grades, AsOf asOf, EntityManager entityManager, Audit audit,
            Out<GradeMerged> mergedOut) throws InvalidGradeException {
        String user = AuditUser.decode(userHeader);
        if (!Role.STEWARD.equals(roles.roleOf(sheet.getId(), user))) {
            throw new InvalidGradeException("Only a steward can merge a grade.");
        }
        if (!RetiredGrades.isGrade(sheet.getId(), request.from(), added)
                || !RetiredGrades.isGrade(sheet.getId(), request.into(), added)) {
            throw new InvalidGradeException("The grades must be the sheet's grades.");
        }
        if (retired.existsBySheetIdAndGrade(sheet.getId(), request.into())) {
            throw new InvalidGradeException("The grade " + request.into() + " is retired.");
        }
        audit.record("GRADE_MERGED sheet=" + sheet.getId() + " by=" + user
                + " from=" + request.from() + " into=" + request.into());
        int merged = 0;
        for (Person person : people.findListed(sheet.getId())) {
            if (!request.from().equals(grades.gradesOf(person).on(sheet.getToday()))) {
                continue;
            }
            Rollup before = Rollup.of(sheet.getId(), sheets, people, claims, rates, grades, asOf);
            // Every save raises the version, as the person form's does.
            entityManager.lock(person, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
            people.saveAndFlush(person);
            PersonGrade dated = grades.findByPersonIdAndFromDate(person.getId(), sheet.getToday())
                    .orElseGet(() -> new PersonGrade(person.getSheetId(), person.getId(), request.into(), sheet.getToday()));
            dated.setGrade(request.into());
            dated.setMadeOn(sheet.getToday());
            grades.saveAndFlush(dated);
            audit.record(PersonRules.editedLine(person, grades.gradesOf(person).on(sheet.getToday()),
                    person.termsOn(sheet.getToday()), user));
            Rollup.of(sheet.getId(), sheets, people, claims, rates, grades, asOf)
                    .changesSince(before, sheet.getId(), user).forEach(audit::record);
            // Retroactive: every grade of theirs on the merged grade, whatever its date, becomes the other.
            if (request.from().equals(person.getGrade())) {
                person.setGrade(request.into());
                people.saveAndFlush(person);
            }
            for (PersonGrade earlier : grades.findByPersonIdOrderByFromDateAsc(person.getId())) {
                if (request.from().equals(earlier.getGrade())) {
                    earlier.setGrade(request.into());
                    grades.saveAndFlush(earlier);
                }
            }
            merged++;
        }
        mergedOut.set(new GradeMerged(merged));
    }
}
