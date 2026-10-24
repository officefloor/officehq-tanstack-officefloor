package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonGradeRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.sheet.Sheet;

/** The claim form's rules, shared by the steps that add claims one at a time and many at once. */
public final class ClaimRules {

    private static final Pattern AMOUNT = Pattern.compile("-?\\d+(\\.\\d{1,2})?");

    private static final Pattern NUMBER = Pattern.compile("(.+-)?\\d{1,9}");

    private ClaimRules() {
    }

    /** Checks one claim form's fields as ValidateClaim does; a null field is blank. */
    public static ClaimInput check(String amountField, String dateField, String noteField, String correctsField)
            throws InvalidClaimException {
        String amount = amountField == null ? "" : amountField.trim();
        String date = dateField == null ? "" : dateField.trim();
        String note = noteField == null ? "" : noteField.trim();
        String corrects = correctsField == null ? "" : correctsField.trim();
        if (!AMOUNT.matcher(amount).matches()) {
            throw new InvalidClaimException("The amount must be a number with up to two decimals.");
        }
        if (!corrects.isEmpty() && !NUMBER.matcher(corrects).matches()) {
            throw new InvalidClaimException("Corrects must be a claim number.");
        }
        BigDecimal value = new BigDecimal(amount).setScale(2);
        if (value.signum() < 0 && corrects.isEmpty()) {
            throw new InvalidClaimException("A negative amount is a correction: name the settled claim it corrects.");
        }
        if (value.signum() >= 0 && !corrects.isEmpty()) {
            throw new InvalidClaimException("Only a correction (a negative amount) corrects a claim.");
        }
        LocalDate parsed = null;
        if (!date.isEmpty()) {
            try {
                parsed = LocalDate.parse(date);
            } catch (DateTimeParseException e) {
                throw new InvalidClaimException("Not a date: " + date);
            }
        }
        return new ClaimInput(value, parsed, note, corrects.isEmpty() ? null : corrects);
    }

    /** The claim the input makes for the person, not yet saved; refused as BuildClaim refuses it. */
    public static Claim make(ClaimInput input, Sheet sheet, Person person, ClaimRepository repository,
            PeriodRepository periods, PersonGradeRepository grades) throws InvalidClaimException {
        LocalDate date = input.date() != null ? input.date() : sheet.getToday();
        if (periods.isClosedOn(sheet.getId(), date)) {
            throw new InvalidClaimException("No claim can be added in a closed period.");
        }
        if (person.retiredOn(date)) {
            throw new InvalidClaimException("No claim can be dated after the person's retirement date.");
        }
        LocalDate correctsDate = repository.checkCorrects(sheet.getId(), person.getId(), input.corrects());
        Claim claim = new Claim(sheet.getId(), person.getId(), input.amount(), date, input.note(), input.corrects());
        claim.setCorrectsDate(correctsDate);
        claim.setLateEntry(periods.isSoftClosedOn(sheet.getId(), date));
        claim.setDueBy(date.plusDays(sheet.termsFor(person.getTerms(), grades.gradesOf(person).on(date), date)));
        return claim;
    }

    /** Records the claim's adding, and after it, when it is a late entry, its LATE_ENTRY. */
    public static void auditAdded(Claim claim, String personName, String by, Audit audit) {
        audit.record(ClaimAudit.line("CLAIM_ADDED", claim, personName, by));
        if (claim.isLateEntry()) {
            audit.record(ClaimAudit.line("LATE_ENTRY", claim, personName, by));
        }
    }

    /** Writes the claim's status log entry as LogClaimStatus does, made by the given user. */
    public static void log(Claim claim, Sheet sheet, ClaimLogRepository log, PersonRepository people,
            RateRepository rates, PersonGradeRepository grades, String by) {
        BigDecimal due = people.findById(claim.getPersonId())
                .map(p -> new ClaimFigures(rates.findBySheetIdOrderByGradeAscFromDateAscIdAsc(claim.getSheetId()),
                        grades.gradesOf(p), sheet).due(claim))
                .orElse(null);
        log.saveAndFlush(new ClaimLog(claim, by, sheet.getToday(), due));
    }
}
