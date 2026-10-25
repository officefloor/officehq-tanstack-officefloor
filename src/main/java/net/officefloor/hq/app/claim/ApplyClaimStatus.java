package net.officefloor.hq.app.claim;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import net.officefloor.hq.app.history.AsOf;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonGradeRepository;
import net.officefloor.hq.app.person.PersonGrades;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.voidreason.VoidReasonRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Moves the claim to the status the action leads to; refuses a change its status does not allow, and an open,
 * settle or void by a role the sheet's permission matrix does not allow it to. Opening records
 * who opened the claim; it takes no number. A claim is numbered when it is settled (or approved): the sheet's next number in the fiscal year of its due-by (FY<year>-<n>, shared across the sheet's tree) (or, while the sheet's period numbering is on, the next in its period: <period>-<n>,
 * X-<n> for a claim in no period); a claim numbered already keeps its number, and a voided claim keeps its number (never reused). The one who opened a
 * claim cannot settle it, and a clerk (any name not made a steward) settles a claim only when its due is at or below
 * the clerk limit in force (its person's grade's on the sheet's today when one is set, else the sheet's; a due of n/a is within any limit; no limit when the sheet has none): a clerk settling one
 * above it leaves it PENDING instead. Only a steward, or its person's approver, approves a PENDING claim (it is settled, its due fixed at its live
 * figure then) or rejects it, with a reason that is not blank (it is OPEN again); the one who opened it cannot approve it. The one who last edited a claim (its form or its amount entry) cannot settle or approve it either. Settling fixes the claim's due at the rate of the grade its
 * person had on the claim's date, in force on the claim's date, at least the sheet's floor (or at the figure its person's last statement bound it to), plus its late charge on the sheet's today (none when no rate applies then, so it stays live). A claim deferred until after the sheet's today cannot be settled. A claim whose due is above the proof threshold in force on the claim's date (its person's grade's on that date when one is set, else the sheet's) cannot be settled or approved while it has no proof, nor while the sheet's proof kinds are set and it lacks a proof of one of them — refused before the clerk limit is considered. A claim whose due is above the sheet's note threshold in force on the claim's date cannot be opened while its note is empty. A claim whose due is above the sheet's review threshold in force on the claim's date goes to REVIEW when opened instead of OPEN: it records who opened it. Only a steward reviews a REVIEW claim, which opens it (OPEN). A claim whose due-by is in a closed period cannot change status. A claim whose due-by is in a period with a cap cannot be opened while its person already has that many OPEN or PENDING claims whose due-by is in it. A DRAFT is deleted (DELETED: it counts nowhere and
 * cannot be edited, opened or voided) and restored (DRAFT again) only on the sheet's today it was deleted (its status log's last DELETED date). Only a steward voids a SETTLED claim, and only within 7 days of
 * its settlement (its status log's SETTLED date) by the sheet's today; it keeps its number and its frozen due. While the sheet's list of
 * void reasons is not empty, a void needs a reason from it, which the claim keeps. Returns the claim's number for the
 * audit. A settle that is part of a bulk settle (its batch key) gives the claim that batch's number (the sheet's next
 * the first time a claim of the batch settles); one settled on its own has none. A settle as of a date (on) on or before
 * the sheet's today freezes the claim's due as it stood at the end of that date (as the as-of view works it out: the
 * claim, its person's grades, the rates and the sheet's settings as they then stood, that date its today), and the
 * rules that weigh the due weigh that figure; a date after the sheet's today, or one by which the claim had not been
 * added, is refused.
 */
public class ApplyClaimStatus {

    /** How many days after its settlement a steward may still void a SETTLED claim. */
    static final int VOID_DAYS = 7;

    public String apply(@PathVariable("action") String action, @Val Claim claim, ClaimRepository repository,
            EntityManager entityManager, PersonRepository people, RateRepository rates, PersonGradeRepository grades, PeriodRepository periods,
            @Val Sheet sheet, RoleRepository roles, ClaimLogRepository log, VoidReasonRepository voidReasons,
            @RequestHeader(name = "X-User", required = false) String userHeader,
            @RequestParam(name = "reason", required = false) String reason,
            @RequestParam(name = "batch", required = false) String batch,
            @RequestParam(name = "on", required = false) String on, AsOf asOf) throws ClaimStatusException, ClerkLimitException, ProofRequiredException {
        SettledAsOf settledAsOf = "settle".equals(action) ? settledAsOf(claim, sheet, on, asOf) : null;
        return run(action, claim, repository, entityManager, people, rates, grades, periods, sheet, roles, log, voidReasons,
                userHeader, reason, batch, false, false, settledAsOf);
    }

    /** The date a settle is made as of, and the claim's due as it stood at the end of it (null: n/a). */
    record SettledAsOf(LocalDate on, BigDecimal due) {
    }

    /** The settle's as-of date and the due then; null when no date is given (the settle is as of the sheet's today). */
    static SettledAsOf settledAsOf(Claim claim, Sheet sheet, String on, AsOf asOf) throws ClaimStatusException {
        if (on == null || on.isBlank()) {
            return null;
        }
        LocalDate date;
        try {
            date = LocalDate.parse(on.trim());
        } catch (DateTimeParseException e) {
            throw new ClaimStatusException("A claim is settled as of a date YYYY-MM-DD.");
        }
        if (date.isAfter(sheet.getToday())) {
            throw new ClaimStatusException("A claim cannot be settled as of a date after the sheet's today.");
        }
        Claim then = asOf.claims(claim.getSheetId(), date).stream().filter(c -> c.getId().equals(claim.getId()))
                .findFirst().orElse(null);
        Person person = then == null ? null : asOf.people(claim.getSheetId(), date).stream()
                .filter(p -> p.getId().equals(then.getPersonId())).findFirst().orElse(null);
        if (then == null || person == null) {
            throw new ClaimStatusException("The claim did not exist on " + date + ".");
        }
        PersonGrades grades = new PersonGrades(person.getGrade(),
                asOf.grades(claim.getSheetId(), date).getOrDefault(person.getId(), List.of()));
        BigDecimal due = new ClaimFigures(asOf.rates(claim.getSheetId(), date), grades, asOf.sheet(sheet, date)).due(then);
        return new SettledAsOf(date, due);
    }

    /** The change itself; on a dry run (a settle only) it refuses what the settle would refuse and changes nothing. */
    static String run(String action, Claim claim, ClaimRepository repository,
            EntityManager entityManager, PersonRepository people, RateRepository rates, PersonGradeRepository grades, PeriodRepository periods,
            Sheet sheet, RoleRepository roles, ClaimLogRepository log, VoidReasonRepository voidReasons,
            String userHeader, String reason, String batch, boolean dryRun) throws ClaimStatusException, ClerkLimitException, ProofRequiredException {
        return run(action, claim, repository, entityManager, people, rates, grades, periods, sheet, roles, log, voidReasons,
                userHeader, reason, batch, dryRun, false);
    }

    /** As above; when the four-eyes and proof rules were already judged on the claim a part-settle split it off from,
     *  they are not judged again on it. */
    static String run(String action, Claim claim, ClaimRepository repository,
            EntityManager entityManager, PersonRepository people, RateRepository rates, PersonGradeRepository grades, PeriodRepository periods,
            Sheet sheet, RoleRepository roles, ClaimLogRepository log, VoidReasonRepository voidReasons,
            String userHeader, String reason, String batch, boolean dryRun, boolean judged) throws ClaimStatusException, ClerkLimitException, ProofRequiredException {
        return run(action, claim, repository, entityManager, people, rates, grades, periods, sheet, roles, log, voidReasons,
                userHeader, reason, batch, dryRun, judged, null);
    }

    /** As above; a settle as of a date fixes the due it stood at then (null: as of the sheet's today). */
    static String run(String action, Claim claim, ClaimRepository repository,
            EntityManager entityManager, PersonRepository people, RateRepository rates, PersonGradeRepository grades, PeriodRepository periods,
            Sheet sheet, RoleRepository roles, ClaimLogRepository log, VoidReasonRepository voidReasons,
            String userHeader, String reason, String batch, boolean dryRun, boolean judged, SettledAsOf settledAsOf)
            throws ClaimStatusException, ClerkLimitException, ProofRequiredException {
        if (dryRun && !"settle".equals(action)) {
            throw new ClaimStatusException("Only a settle can be checked.");
        }
        String user = AuditUser.decode(userHeader);
        if (periods.isClosedOn(claim.getSheetId(), claim.getDueBy())) {
            throw new ClaimStatusException("A claim in a closed period cannot change status.");
        }
        String role = roles.roleOf(claim.getSheetId(), user);
        if (("open".equals(action) || "settle".equals(action) || "void".equals(action)) && !sheet.allows(action, role)) {
            throw new ClerkLimitException("A " + role + " cannot " + action + " a claim.");
        }
        if ("open".equals(action) && !Role.STEWARD.equals(role) && people.findById(claim.getPersonId())
                .map(p -> p.flaggedOn(sheet.getToday())).orElse(false)) {
            throw new ClerkLimitException("Only a steward can open a claim of a flagged person.");
        }
        if ("review".equals(action) && claim.getStatus().after(action) != null && !Role.STEWARD.equals(role)) {
            throw new ClerkLimitException("Only a steward can review a claim.");
        }
        if (("open".equals(action) || "review".equals(action)) && claim.getStatus().after(action) != null) {
            // Lock the sheet first, then re-read the claim so a concurrent open is seen.
            boolean perPeriod = sheet.periodNumberingOn();
            if (perPeriod) {
                repository.lockSheet(claim.getSheetId());
            } else {
                repository.lockTree(claim.getSheetId());
            }
            entityManager.refresh(claim);
            ClaimStatus next = check(claim, action);
            BigDecimal due = people.findById(claim.getPersonId())
                    .map(p -> new ClaimFigures(rates.findInChain(claim.getSheetId()),
                            grades.gradesOf(p), sheet).due(claim))
                    .orElse(null);
            BigDecimal noteThreshold = sheet.noteThresholdOn(claim.getDate());
            if ("open".equals(action) && noteThreshold != null && (claim.getNote() == null || claim.getNote().isBlank())) {
                if (due != null && due.compareTo(noteThreshold) > 0) {
                    throw new ClaimStatusException("A claim whose due is above the note threshold needs a note to be opened.");
                }
            }
            Period capped = periods.findBySheetIdOrderByFromDateAscIdAsc(claim.getSheetId()).stream()
                    .filter(p -> p.getCap() != null && claim.getDueBy() != null && p.contains(claim.getDueBy()))
                    .findFirst().orElse(null);
            if (capped != null) {
                long open = repository.findBySheetIdAndPersonIdOrderByIdDesc(claim.getSheetId(), claim.getPersonId()).stream()
                        .filter(c -> !c.getId().equals(claim.getId()))
                        .filter(c -> c.getStatus() == ClaimStatus.OPEN || c.getStatus() == ClaimStatus.PENDING)
                        .filter(c -> c.getDueBy() != null && capped.contains(c.getDueBy()))
                        .count();
                if (open >= capped.getCap()) {
                    throw new ClaimStatusException("The person already has " + capped.getCap()
                            + " open claims in period " + capped.getName() + ".");
                }
            }
            Integer maxOpen = sheet.getMaxOpen();
            if ("open".equals(action) && maxOpen != null) {
                long open = repository.findBySheetIdAndPersonIdOrderByIdDesc(claim.getSheetId(), claim.getPersonId()).stream()
                        .filter(c -> !c.getId().equals(claim.getId()))
                        .filter(c -> c.getStatus() == ClaimStatus.OPEN || c.getStatus() == ClaimStatus.PENDING)
                        .count();
                if (open >= maxOpen) {
                    throw new ClaimStatusException("The person already has " + maxOpen + " open claims.");
                }
            }
            BigDecimal reviewThreshold = sheet.reviewThresholdOn(claim.getDate());
            if ("open".equals(action) && reviewThreshold != null && due != null && due.compareTo(reviewThreshold) > 0) {
                claim.setStatus(ClaimStatus.REVIEW);
                claim.setReviewOn(sheet.getToday());
                claim.setOpenedBy(user);
                repository.saveAndFlush(claim);
                return null;
            }
            claim.setStatus(next);
            if ("open".equals(action)) {
                claim.setOpenedBy(user);
            }
        } else {
            ClaimStatus next = check(claim, action);
            boolean steward = Role.STEWARD.equals(role);
            if (("approve".equals(action) || "reject".equals(action)) && !steward && !people.findById(claim.getPersonId())
                    .map(p -> user.equals(p.getApprover())).orElse(false)) {
                throw new ClerkLimitException("Only a steward or the person's approver can approve or reject a pending claim.");
            }
            if ("void".equals(action) && claim.getStatus() == ClaimStatus.SETTLED) {
                if (!steward) {
                    throw new ClerkLimitException("Only a steward can void a settled claim.");
                }
                LocalDate settled = log.settledOn(claim);
                if (settled == null || sheet.getToday().isAfter(settled.plusDays(VOID_DAYS))) {
                    throw new ClaimStatusException("A settled claim can be voided only within " + VOID_DAYS + " days of its settlement.");
                }
            }
            if ("void".equals(action) && voidReasons.existsBySheetId(claim.getSheetId())) {
                String chosen = reason == null ? "" : reason.trim();
                if (chosen.isEmpty() || !voidReasons.existsBySheetIdAndReason(claim.getSheetId(), chosen)) {
                    throw new ClaimStatusException("A void needs a reason from the sheet's list.");
                }
                claim.setVoidReason(chosen);
            }
            if ("restore".equals(action)) {
                LocalDate deleted = log.deletedOn(claim);
                if (deleted == null || !deleted.equals(sheet.getToday())) {
                    throw new ClaimStatusException("A deleted claim can be restored only on the sheet's today it was deleted.");
                }
            }
            if ("reject".equals(action) && (reason == null || reason.isBlank())) {
                throw new ClaimStatusException("A rejection needs a reason.");
            }
            if (next == ClaimStatus.SETTLED && claim.isDeferredOn(sheet.getToday())) {
                throw new ClaimStatusException("A deferred claim cannot be settled until the sheet's today reaches its date.");
            }
            if (next == ClaimStatus.SETTLED && !judged && user.equals(claim.getOpenedBy())) {
                throw new ClaimStatusException("The one who opened a claim cannot settle or approve it.");
            }
            if (next == ClaimStatus.SETTLED && !judged && user.equals(claim.getEditedBy())) {
                throw new ClaimStatusException("The one who last edited a claim cannot settle or approve it.");
            }
            if (next == ClaimStatus.SETTLED) {
                PersonGrades personGrades = people.findById(claim.getPersonId()).map(grades::gradesOf).orElse(null);
                BigDecimal due = settledAsOf != null && "settle".equals(action) ? settledAsOf.due()
                        : personGrades == null ? null
                        : new ClaimFigures(rates.findInChain(claim.getSheetId()),
                                personGrades, sheet).due(claim);
                BigDecimal threshold = sheet.proofThresholdFor(
                        personGrades == null ? null : personGrades.on(claim.getDate()), claim.getDate());
                if (!judged && threshold != null && due != null && due.compareTo(threshold) > 0 && claim.getProofs() == 0) {
                    throw new ProofRequiredException("A claim whose due is above the proof threshold needs a proof to be settled or approved.");
                }
                if (!judged && threshold != null && due != null && due.compareTo(threshold) > 0) {
                    for (String kind : sheet.getProofKinds()) {
                        if (claim.proofsOf(kind) == 0) {
                            throw new ProofRequiredException("A claim whose due is above the proof threshold needs a proof of each of the sheet's proof kinds (" + kind + " missing) to be settled or approved.");
                        }
                    }
                }
                if (dryRun) {
                    return claim.getNumber();
                }
                BigDecimal limit = sheet.clerkLimitFor(personGrades == null ? null : personGrades.on(sheet.getToday()));
                if ("settle".equals(action) && limit != null && due != null && due.compareTo(limit) > 0 && !steward) {
                    next = ClaimStatus.PENDING;
                } else {
                    claim.setDue(due);
                    if (claim.getNumber() == null) {
                        claim.setNumber(takeNumber(claim, repository, periods, sheet));
                    }
                }
                if ("settle".equals(action)) {
                    String key = batch == null ? "" : batch.trim();
                    claim.setSettleBatch(key.isEmpty() ? null : repository.batchFor(claim.getSheetId(), key));
                }
            }
            claim.setStatus(next);
        }
        repository.saveAndFlush(claim);
        return claim.getNumber();
    }

    /** The next number in the claim's sequence as it is settled now: within the period its due-by falls in
     *  (<period>-<n>, X-<n> in none) while the sheet's period numbering is on, else in the fiscal year of its due-by
     *  (FY<year>-<n>, shared across the sheet's tree). Locks the sheet (or tree) first, so two claims settled at once
     *  are numbered one after the other. */
    private static String takeNumber(Claim claim, ClaimRepository repository, PeriodRepository periods, Sheet sheet) {
        if (sheet.periodNumberingOn()) {
            repository.lockSheet(claim.getSheetId());
            String prefix = periods.findBySheetIdOrderByFromDateAscIdAsc(claim.getSheetId()).stream()
                    .filter(p -> p.contains(claim.getDueBy())).map(Period::getName).findFirst().orElse(null);
            String seq = prefix == null ? "X" : "P:" + prefix;
            return (prefix == null ? "X" : prefix) + "-" + repository.takeNextInSequence(claim.getSheetId(), seq);
        }
        repository.lockTree(claim.getSheetId());
        return repository.takeNextInFiscalYear(claim.getSheetId(), sheet.getFiscalStartMonth(), claim.getDueBy());
    }

    private static ClaimStatus check(Claim claim, String action) throws ClaimStatusException {
        ClaimStatus next = claim.getStatus().after(action);
        if (next == null) {
            throw new ClaimStatusException("A " + claim.getStatus() + " claim cannot " + action + ".");
        }
        return next;
    }
}
