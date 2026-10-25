package net.officefloor.hq.app.claim;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.regex.Pattern;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.PersonGradeRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Splits an amount (more than 0.00 and less than the claim's amount) off an OPEN claim. The claim keeps the rest and
 * loses any figure a statement bound it to; a new OPEN claim of the same person gets the split-off amount, the same
 * date and note, the next number (as opening gives one), the signed-in user as who opened it, a due-by fixed now from
 * the terms in force on the date, the original's number as the one it was split from, and the original's tags. A user whose role the sheet's permission matrix does not allow to open a claim cannot split one (the split opens
 * the new claim). A PENDING, deferred, settled
 * or any other non-OPEN claim, or one whose due-by is in a closed period, is refused. The new claim is numbered in the period its own due-by falls in, and is a late entry when that period is soft-closed. Returns the split, for the audit.
 */
public class ApplySplit {

    private static final Pattern AMOUNT = Pattern.compile("\\d+(\\.\\d{1,2})?");

    public Split apply(@Val Claim claim, @Val Sheet sheet, ClaimRepository repository, EntityManager entityManager,
            PeriodRepository periods, PersonRepository people, PersonGradeRepository grades, RateRepository rates,
            ClaimLogRepository log, HolidayRepository holidays, RoleRepository roles,
            @RequestHeader(name = "X-User", required = false) String userHeader,
            @RequestParam(name = "amount", required = false) String amountParam) throws ClaimStatusException {
        String user = AuditUser.decode(userHeader);
        String amountField = amountParam == null ? "" : amountParam.trim();
        if (!AMOUNT.matcher(amountField).matches()) {
            throw new ClaimStatusException("The amount to split off must be a number with up to two decimals.");
        }
        BigDecimal amount = new BigDecimal(amountField).setScale(2);
        String role = roles.roleOf(claim.getSheetId(), user);
        if (!sheet.allows("open", role)) {
            throw new ClaimStatusException("A " + role + " cannot open a claim, so cannot split one.");
        }
        if (periods.isClosedOn(claim.getSheetId(), claim.getDueBy())) {
            throw new ClaimStatusException("A claim in a closed period cannot be split.");
        }
        // Lock the sheet first (taking a number locks it), then re-read the claim so a concurrent change is seen.
        boolean perPeriod = sheet.periodNumberingOn();
        if (perPeriod) {
            repository.lockSheet(claim.getSheetId());
        } else {
            repository.lockTree(claim.getSheetId());
        }
        entityManager.refresh(claim);
        if (claim.getStatus() != ClaimStatus.OPEN || claim.isDeferredOn(sheet.getToday())) {
            throw new ClaimStatusException("Only an OPEN claim that is not deferred can be split.");
        }
        if (amount.signum() <= 0 || amount.compareTo(claim.getAmount()) >= 0) {
            throw new ClaimStatusException("The amount to split off must be more than 0.00 and less than the claim's.");
        }
        int terms = people.findById(claim.getPersonId())
                .map(p -> sheet.termsFor(p.termsOn(sheet.getToday()), grades.gradesOf(p).on(sheet.getToday()), sheet.getToday())).orElse(0);
        LocalDate dueBy = holidays.dueBy(claim.getSheetId(), claim.getDate(), terms);
        String number;
        if (perPeriod) {
            String prefix = periods.findBySheetIdOrderByFromDateAscIdAsc(claim.getSheetId()).stream()
                    .filter(p -> p.contains(dueBy)).map(Period::getName).findFirst().orElse(null);
            String seq = prefix == null ? "X" : "P:" + prefix;
            number = (prefix == null ? "X" : prefix) + "-" + repository.takeNextInSequence(claim.getSheetId(), seq);
        } else {
            number = repository.takeNextInFiscalYear(claim.getSheetId(), sheet.getFiscalStartMonth(), dueBy);
        }
        claim.setAmount(claim.getAmount().subtract(amount));
        claim.bind(false, null);
        repository.saveAndFlush(claim);

        Claim made = new Claim(claim.getSheetId(), claim.getPersonId(), amount, claim.getDate(), claim.getNote(), null);
        made.setStatus(ClaimStatus.OPEN);
        made.setNumber(number);
        made.setOpenedBy(user);
        made.setSplitFrom(claim.getNumber());
        made.setTags(claim.getTags());
        made.fixDue(claim.getDate(), terms, holidays.datesOf(claim.getSheetId()));
        made.setLateEntry(periods.isSoftClosedOn(claim.getSheetId(), dueBy));
        made = repository.saveAndFlush(made);
        entityManager.refresh(made);
        ClaimRules.log(made, sheet, log, people, rates, grades, user);
        return new Split(made);
    }
}
