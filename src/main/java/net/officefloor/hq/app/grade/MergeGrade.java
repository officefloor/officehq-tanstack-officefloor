package net.officefloor.hq.app.grade;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.role.StewardOnlyException;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Merges a grade into another (a steward): everyone whose grade in force on the sheet's today is the
 * merged grade is given the other from the sheet's today, in list order, exactly as the person form
 * would. A retired grade cannot be merged into. Recorded as GRADE_MERGED, then each person graded as a
 * single edit is: its PERSON_EDITED line, then the figures it moved.
 */
public class MergeGrade {
    public void merge(@Val GradeMergeRequest request, @Val Sheet sheet, PersonRepository people,
            ClaimRepository claims, RateRepository rates, SettingsRepository settings, RoleRepository roles,
            GradeRepository grades, HttpServletRequest http, Audit audit, Out<GradeMergeResponse> result)
            throws StewardOnlyException, InvalidGradeException {
        String user = Audit.user(http);
        if (!Role.STEWARD.equals(roles.roleOf(sheet.id(), user))) {
            throw new StewardOnlyException("Only a steward may merge grades");
        }
        if (grades.isRetired(sheet.id(), request.into())) {
            throw new InvalidGradeException("Grade " + request.into() + " is retired");
        }
        audit.record("GRADE_MERGED sheet=" + sheet.id() + " by=" + user + " from=" + request.from()
                + " into=" + request.into());
        Rollup before = rollup(sheet, people, claims, rates, settings);
        int merged = 0;
        for (Person person : people.findAll(sheet.id())) {
            if (!request.from().equals(person.gradeOn(sheet.today()))) continue;
            person.setGradeFrom(request.into(), sheet.today());
            // Saved over by someone else since it was read: left as it is.
            if (!people.save(person)) continue;
            merged++;
            audit.record("PERSON_EDITED sheet=" + sheet.id() + " by=" + user + " name=" + person.getName()
                    + " grade=" + person.gradeOn(sheet.today()));
            Rollup after = rollup(sheet, people, claims, rates, settings);
            after.changesSince(before, sheet.id(), user).forEach(audit::record);
            before = after;
        }
        result.set(new GradeMergeResponse(merged));
    }

    private static Rollup rollup(Sheet sheet, PersonRepository people, ClaimRepository claims, RateRepository rates,
            SettingsRepository settings) {
        return Rollup.of(sheet.id(), sheet.today(), people, claims, rates, settings.find(sheet.id()));
    }
}
