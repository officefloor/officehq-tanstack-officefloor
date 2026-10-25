package net.officefloor.hq.app.quickadd;

import java.time.LocalDate;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.PersonAliasRepository;
import net.officefloor.hq.app.person.PersonGradeRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Refuses the quick-add before anyone is added when its claim, dated the sheet's entry date (else its today), would have
 *  its due-by in a closed period: the named person's terms-th business day after the date, as BuildClaim fixes it (a
 *  person not on the sheet yet is added with the sheet's default grade and no terms of their own) — so a refused claim leaves no new person behind. */
public class CheckQuickAddDate {
    public void check(@Val QuickAddName name, @Val Sheet sheet, PeriodRepository periods, PersonRepository people,
            PersonAliasRepository aliases,
            PersonGradeRepository grades, HolidayRepository holidays) throws InvalidClaimException {
        LocalDate date = sheet.entryDateInForce();
        int terms = PersonAliasRepository.findByNameOrAlias(people.findListed(sheet.getId()),
                        aliases, sheet.getId(), name.name())
                .map(p -> sheet.termsFor(p.termsOn(sheet.getToday()), grades.gradesOf(p).on(sheet.getToday()), sheet.getToday()))
                .orElseGet(() -> sheet.termsFor(null, sheet.getDefaultGrade(), sheet.getToday()));
        if (periods.isClosedOn(sheet.getId(), holidays.dueBy(sheet.getId(), date, terms))) {
            throw new InvalidClaimException("No claim can be added in a closed period.");
        }
    }
}
