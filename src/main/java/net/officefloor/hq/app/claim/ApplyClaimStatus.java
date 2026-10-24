package net.officefloor.hq.app.claim;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
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
 * settle or void by a role the sheet's permission matrix does not allow it to. Opening gives
 * the claim the sheet's next number (or, while the sheet's period numbering is on, the next in its period: <period>-<n>,
 * X-<n> for a claim in no period) and records who opened it; a voided claim keeps its number (never reused). The one who opened a
 * claim cannot settle it, and a clerk (any name not made a steward) settles a claim only when its due is at or below
 * the sheet's clerk limit (a due of n/a is within any limit; no limit when the sheet has none): a clerk settling one
 * above it leaves it PENDING instead. Only a steward approves a PENDING claim (it is settled, its due fixed at its live
 * figure then) or rejects it, with a reason that is not blank (it is OPEN again); the one who opened it cannot approve it. The one who last edited a claim (its form or its amount entry) cannot settle or approve it either. Settling fixes the claim's due at the rate of the grade its
 * person had on the claim's date, in force on the claim's date, at least the sheet's floor (or at the figure its person's last statement bound it to), plus its late charge on the sheet's today (none when no rate applies then, so it stays live). A claim deferred until after the sheet's today cannot be settled. A claim whose due is above the proof threshold in force on the claim's date (its person's grade's on that date when one is set, else the sheet's) cannot be settled or approved while it has no proof — refused before the clerk limit is considered. A claim whose due is above the sheet's note threshold in force on the claim's date cannot be opened while its note is empty. A claim whose due-by is in a closed period cannot change status. Only a steward voids a SETTLED claim, and only within 7 days of
 * its settlement (its status log's SETTLED date) by the sheet's today; it keeps its number and its frozen due. While the sheet's list of
 * void reasons is not empty, a void needs a reason from it, which the claim keeps. Returns the claim's number for the
 * audit. A settle that is part of a bulk settle (its batch key) gives the claim that batch's number (the sheet's next
 * the first time a claim of the batch settles); one settled on its own has none.
 */
public class ApplyClaimStatus {

    /** How many days after its settlement a steward may still void a SETTLED claim. */
    static final int VOID_DAYS = 7;

    public String apply(@PathVariable("action") String action, @Val Claim claim, ClaimRepository repository,
            EntityManager entityManager, PersonRepository people, RateRepository rates, PersonGradeRepository grades, PeriodRepository periods,
            @Val Sheet sheet, RoleRepository roles, ClaimLogRepository log, VoidReasonRepository voidReasons,
            @RequestHeader(name = "X-User", required = false) String userHeader,
            @RequestParam(name = "reason", required = false) String reason,
            @RequestParam(name = "batch", required = false) String batch) throws ClaimStatusException, ClerkLimitException, ProofRequiredException {
        return run(action, claim, repository, entityManager, people, rates, grades, periods, sheet, roles, log, voidReasons,
                userHeader, reason, batch, false);
    }

    /** The change itself; on a dry run (a settle only) it refuses what the settle would refuse and changes nothing. */
    static String run(String action, Claim claim, ClaimRepository repository,
            EntityManager entityManager, PersonRepository people, RateRepository rates, PersonGradeRepository grades, PeriodRepository periods,
            Sheet sheet, RoleRepository roles, ClaimLogRepository log, VoidReasonRepository voidReasons,
            String userHeader, String reason, String batch, boolean dryRun) throws ClaimStatusException, ClerkLimitException, ProofRequiredException {
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
        if ("open".equals(action) && claim.getStatus().after(action) != null) {
            // Lock the sheet first (taking a number locks it), then re-read the claim so a concurrent open is seen.
            boolean perPeriod = sheet.periodNumberingOn();
            if (perPeriod) {
                repository.lockSheet(claim.getSheetId());
            } else {
                repository.takeNextNumber(claim.getSheetId());
            }
            entityManager.refresh(claim);
            ClaimStatus next = check(claim, action);
            BigDecimal noteThreshold = sheet.noteThresholdOn(claim.getDate());
            if (noteThreshold != null && (claim.getNote() == null || claim.getNote().isBlank())) {
                BigDecimal due = people.findById(claim.getPersonId())
                        .map(p -> new ClaimFigures(rates.findInChain(claim.getSheetId()),
                                grades.gradesOf(p), sheet).due(claim))
                        .orElse(null);
                if (due != null && due.compareTo(noteThreshold) > 0) {
                    throw new ClaimStatusException("A claim whose due is above the note threshold needs a note to be opened.");
                }
            }
            if (perPeriod) {
                String prefix = periods.findBySheetIdOrderByFromDateAscIdAsc(claim.getSheetId()).stream()
                        .filter(p -> p.contains(claim.getDueBy())).map(Period::getName).findFirst().orElse(null);
                String seq = prefix == null ? "X" : "P:" + prefix;
                claim.setNumber((prefix == null ? "X" : prefix) + "-" + repository.takeNextInSequence(claim.getSheetId(), seq));
            } else {
                claim.setNumber(String.valueOf(repository.lastNumber(claim.getSheetId())));
            }
            claim.setStatus(next);
            claim.setOpenedBy(user);
        } else {
            ClaimStatus next = check(claim, action);
            boolean steward = Role.STEWARD.equals(role);
            if (("approve".equals(action) || "reject".equals(action)) && !steward) {
                throw new ClerkLimitException("Only a steward can approve or reject a pending claim.");
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
            if ("reject".equals(action) && (reason == null || reason.isBlank())) {
                throw new ClaimStatusException("A rejection needs a reason.");
            }
            if (next == ClaimStatus.SETTLED && claim.isDeferredOn(sheet.getToday())) {
                throw new ClaimStatusException("A deferred claim cannot be settled until the sheet's today reaches its date.");
            }
            if (next == ClaimStatus.SETTLED && user.equals(claim.getOpenedBy())) {
                throw new ClaimStatusException("The one who opened a claim cannot settle or approve it.");
            }
            if (next == ClaimStatus.SETTLED && user.equals(claim.getEditedBy())) {
                throw new ClaimStatusException("The one who last edited a claim cannot settle or approve it.");
            }
            if (next == ClaimStatus.SETTLED) {
                PersonGrades personGrades = people.findById(claim.getPersonId()).map(grades::gradesOf).orElse(null);
                BigDecimal due = personGrades == null ? null
                        : new ClaimFigures(rates.findInChain(claim.getSheetId()),
                                personGrades, sheet).due(claim);
                BigDecimal threshold = sheet.proofThresholdFor(
                        personGrades == null ? null : personGrades.on(claim.getDate()), claim.getDate());
                if (threshold != null && due != null && due.compareTo(threshold) > 0 && claim.getProofs() == 0) {
                    throw new ProofRequiredException("A claim whose due is above the proof threshold needs a proof to be settled or approved.");
                }
                if (dryRun) {
                    return claim.getNumber();
                }
                BigDecimal limit = sheet.getClerkLimit();
                if ("settle".equals(action) && limit != null && due != null && due.compareTo(limit) > 0 && !steward) {
                    next = ClaimStatus.PENDING;
                } else {
                    claim.setDue(due);
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

    private static ClaimStatus check(Claim claim, String action) throws ClaimStatusException {
        ClaimStatus next = claim.getStatus().after(action);
        if (next == null) {
            throw new ClaimStatusException("A " + claim.getStatus() + " claim cannot " + action + ".");
        }
        return next;
    }
}
