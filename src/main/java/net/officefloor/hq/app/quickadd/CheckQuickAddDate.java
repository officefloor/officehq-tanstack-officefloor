package net.officefloor.hq.app.quickadd;

import java.time.LocalDate;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.PersonGradeRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Refuses the quick-add before anyone is added when its claim, dated the sheet's entry date (else its today), would have
 *  its due-by in a closed period: the date plus the named person's terms (a person not on the sheet yet is added with
 *  grade A and no terms of their own) — so a refused claim leaves no new person behind. */
public class CheckQuickAddDate {
    public void check(@Val QuickAddName name, @Val Sheet sheet, PeriodRepository periods, PersonRepository people,
            PersonGradeRepository grades) throws InvalidClaimException {
        LocalDate date = sheet.entryDateInForce();
        int terms = people.findBySheetIdOrderByIdAsc(sheet.getId()).stream()
                .filter(p -> p.getName().equalsIgnoreCase(name.name())).findFirst()
                .map(p -> sheet.termsFor(p.getTerms(), grades.gradesOf(p).on(date), date))
                .orElseGet(() -> sheet.termsFor(null, "A", date));
        if (periods.isClosedOn(sheet.getId(), date.plusDays(terms))) {
            throw new InvalidClaimException("No claim can be added in a closed period.");
        }
    }
}
