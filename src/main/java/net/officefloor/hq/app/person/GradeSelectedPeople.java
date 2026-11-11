package net.officefloor.hq.app.person;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.grade.GradeRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Gives each selected person, in list order (the order they were added), the grade from the
 * from-date exactly as the person form would (see {@link ApplyPerson}): their name and terms are
 * kept, and a grade left as the row shows it with the from-date blank records nothing. Each is
 * audited as a single edit is: its PERSON_EDITED line, then the figures it moved. A viewer, or anyone
 * on an archived sheet, has every selected person refused: none is graded and nothing is written.
 */
public class GradeSelectedPeople {
    public void grade(@Val Sheet sheet, @Val BulkGradeRequest request, PersonRepository people,
            ClaimRepository claims, RateRepository rates, SettingsRepository settings, RoleRepository roles, HttpServletRequest http,
            GradeRepository grades, Audit audit, Out<BulkGradeResponse> result) {
        String user = Audit.user(http);
        if (sheet.archived() || Role.VIEWER.equals(roles.roleOf(sheet.id(), user))) {
            result.set(new BulkGradeResponse(0));
            return;
        }
        Set<Long> selected = new HashSet<>();
        for (String id : request.personIds()) {
            try {
                selected.add(Long.parseLong(id.trim()));
            } catch (NumberFormatException | NullPointerException e) {
                // not an id: no person on the sheet
            }
        }
        boolean dated = !request.gradeFrom().isEmpty();
        boolean retired = grades.isRetired(sheet.id(), request.grade());
        Rollup before = rollup(sheet, people, claims, rates, settings);
        int graded = 0;
        for (Person person : people.findAll(sheet.id())) {
            if (!selected.contains(person.getId())) continue;
            // A retired grade is chosen only for someone not already on it: they are refused.
            if (retired && (dated || !request.grade().equals(person.gradeOn(sheet.today())))) continue;
            if (dated || !request.grade().equals(person.gradeOn(sheet.today()))) {
                person.setGradeFrom(request.grade(), dated ? LocalDate.parse(request.gradeFrom()) : sheet.today());
            }
            // Saved over by someone else since it was read: left as it is.
            if (!people.save(person)) continue;
            graded++;
            audit.record("PERSON_EDITED sheet=" + sheet.id() + " by=" + user + " name=" + person.getName()
                    + " grade=" + person.gradeOn(sheet.today()));
            Rollup after = rollup(sheet, people, claims, rates, settings);
            after.changesSince(before, sheet.id(), user).forEach(audit::record);
            before = after;
        }
        result.set(new BulkGradeResponse(graded));
    }

    private static Rollup rollup(Sheet sheet, PersonRepository people, ClaimRepository claims, RateRepository rates,
            SettingsRepository settings) {
        return Rollup.of(sheet.id(), sheet.today(), people, claims, rates, settings.find(sheet.id()));
    }
}
