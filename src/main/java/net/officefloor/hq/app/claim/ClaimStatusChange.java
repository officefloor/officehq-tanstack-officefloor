package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodGuard;
import net.officefloor.hq.app.person.WatchRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.settings.SettingsRepository;

/** A claim's status change (open — to REVIEW instead when its due is above the review threshold in force on its date —,
 *  review (a steward makes a REVIEW claim OPEN), settle, approve, reject or void — a SETTLED claim too, by a steward within 7 days of its settlement), shared by the single and the bulk actions. Every
 *  check is made before anything is written, so a refusal writes nothing to the store or the audit. */
public final class ClaimStatusChange {
    private ClaimStatusChange() {
    }

    /** Open a DRAFT claim exactly as its open button does (for a paste, which opens what it adds). */
    public static Claim open(String id, long cid, String by, ClaimRepository repository, PeriodRepository periods,
            SettingsRepository settings, Audit audit, Rollup rollup, WatchRepository watches) throws BadRequestException {
        return apply(id, cid, "open", by, repository, periods, settings, audit, rollup, watches);
    }

    static Claim apply(String id, long cid, String action, String by, ClaimRepository repository,
            PeriodRepository periods, SettingsRepository settings, Audit audit, Rollup rollup, WatchRepository watches) throws BadRequestException {
        return apply(id, cid, action, null, by, repository, periods, settings, audit, rollup, watches);
    }

    static Claim apply(String id, long cid, String action, String reason, String by, ClaimRepository repository,
            PeriodRepository periods, SettingsRepository settings, Audit audit, Rollup rollup, WatchRepository watches) throws BadRequestException {
        return apply(id, cid, action, reason, by, false, repository, periods, settings, audit, rollup, watches);
    }

    /** Make every check of the status change, writing nothing: throws the refusal the change would give. */
    static void check(String id, long cid, String action, String by, ClaimRepository repository,
            PeriodRepository periods, SettingsRepository settings) throws BadRequestException {
        apply(id, cid, action, null, by, true, repository, periods, settings, null, null, null);
    }

    private static Claim apply(String id, long cid, String action, String reason, String by, boolean checkOnly, ClaimRepository repository,
            PeriodRepository periods, SettingsRepository settings, Audit audit, Rollup rollup, WatchRepository watches) throws BadRequestException {
        if (settings.archived(id)) {
            throw new BadRequestException("the sheet is archived");
        }
        if ("resettle".equals(action == null ? "" : action.trim())) {
            return resettle(id, cid, by, repository, periods, settings, audit, rollup, watches);
        }
        String to;
        List<String> from;
        String event;
        switch (action == null ? "" : action.trim()) {
            case "open" -> { to = "OPEN"; from = List.of("DRAFT"); event = "CLAIM_OPENED"; }
            case "settle" -> { to = "SETTLED"; from = List.of("OPEN"); event = "CLAIM_SETTLED"; }
            case "approve" -> { to = "SETTLED"; from = List.of("PENDING"); event = "CLAIM_APPROVED"; }
            case "reject" -> { to = "OPEN"; from = List.of("PENDING"); event = "CLAIM_REJECTED"; }
            case "review" -> { to = "OPEN"; from = List.of("REVIEW"); event = "CLAIM_OPENED"; }
            case "void" -> { to = "VOID"; from = List.of("DRAFT", "REVIEW", "OPEN", "PENDING", "SETTLED"); event = "CLAIM_VOIDED"; }
            default -> throw new BadRequestException("no such action");
        }
        Claim found = repository.find(id, cid);
        if (found == null) {
            throw new BadRequestException("no such claim");
        }
        if (!from.contains(found.status())) {
            throw new BadRequestException("a " + found.status() + " claim cannot be moved to " + to);
        }
        PeriodGuard.open(periods, id, LocalDate.parse(found.dueBy()));
        // Open, settle and void go by the sheet's permission matrix; approve and reject stay with stewards (below).
        String act = action.trim();
        if ((act.equals("open") || act.equals("settle") || act.equals("void")) && !settings.allows(id, act, by)) {
            throw new BadRequestException("a " + settings.role(id, by) + " cannot " + act + " a claim");
        }
        // Only a steward reviews a REVIEW claim, making it OPEN.
        if (act.equals("review") && !settings.role(id, by).equals(SettingsRepository.STEWARD)) {
            throw new BadRequestException("only a steward can review a claim");
        }
        // Four eyes: whoever opened a claim cannot settle it.
        if (to.equals("SETTLED") && found.status().equals("OPEN") && by.equals(found.openedBy())) {
            throw new BadRequestException("the user who opened a claim cannot settle it");
        }
        // Nor can whoever last edited it settle or approve it.
        if (to.equals("SETTLED") && (found.status().equals("OPEN") || found.status().equals("PENDING"))
                && by.equals(repository.editedBy(id, cid))) {
            throw new BadRequestException("the user who last edited a claim cannot " + act + " it");
        }
        // A deferred claim cannot be settled until the sheet's today reaches the date it is deferred until.
        if (to.equals("SETTLED") && found.deferred()) {
            throw new BadRequestException("a claim deferred until " + found.deferredUntil() + " cannot be settled");
        }
        // Only a steward approves or rejects a PENDING claim, and never one they opened themselves.
        if (found.status().equals("PENDING") && !to.equals("VOID")) {
            if (!settings.role(id, by).equals(SettingsRepository.STEWARD)) {
                throw new BadRequestException("only a steward can " + action.trim() + " a pending claim");
            }
            if (to.equals("SETTLED") && by.equals(found.openedBy())) {
                throw new BadRequestException("the user who opened a claim cannot approve it");
            }
        }
        // Only a steward voids a SETTLED claim, and only within 7 days of the date its status log settled it.
        boolean reversing = found.status().equals("SETTLED") && to.equals("VOID");
        if (reversing) {
            if (!settings.role(id, by).equals(SettingsRepository.STEWARD)) {
                throw new BadRequestException("only a steward can void a settled claim");
            }
            Long days = repository.daysSinceSettled(id, cid);
            if (days == null || days < 0 || days > 7) {
                throw new BadRequestException("a settled claim can be voided only within 7 days of its settlement");
            }
        }
        String why = reason == null ? "" : reason.trim();
        if (event.equals("CLAIM_REJECTED") && why.isEmpty()) {
            throw new BadRequestException("give a reason to reject the claim");
        }
        // While the sheet has void reasons, a void needs one chosen from them; with none, a void takes no reason.
        String voidReason = null;
        if (to.equals("VOID")) {
            List<String> reasons = settings.voidReasons(id);
            if (!reasons.isEmpty()) {
                if (why.isEmpty()) {
                    throw new BadRequestException("choose a reason to void the claim");
                }
                if (!reasons.contains(why)) {
                    throw new BadRequestException(why + " is not one of the sheet's void reasons");
                }
                voidReason = why;
            }
        }
        // A claim whose due is above the note threshold in force on its date cannot be opened with an empty
        // note ('n/a' is above none).
        if (to.equals("OPEN") && found.status().equals("DRAFT") && (found.note() == null || found.note().trim().isEmpty())) {
            BigDecimal threshold = settings.noteThreshold(id, LocalDate.parse(found.date()));
            BigDecimal now = repository.dueNow(id, found);
            if (threshold != null && now != null && now.compareTo(threshold) > 0) {
                throw new BadRequestException("a claim with a due above the note threshold needs a note to be opened");
            }
        }
        // A claim whose due is above the proof threshold in force on its date (its person's grade's then when one
        // is set, else the sheet's) cannot be settled or approved without a proof ('n/a' is above none); this is
        // decided before the clerk limit.
        if (to.equals("SETTLED") && found.proofs() == 0) {
            BigDecimal threshold = settings.proofThreshold(id, repository.gradeOn(id, found), LocalDate.parse(found.date()));
            BigDecimal now = repository.dueNow(id, found);
            if (threshold != null && now != null && now.compareTo(threshold) > 0) {
                throw new BadRequestException("a claim with a due above the proof threshold needs a proof to be settled");
            }
        }
        // A clerk settles outright only a claim whose due is within the sheet's clerk limit ('n/a' is within
        // any); above it the claim goes to PENDING for a steward to approve or reject. A steward settles anything.
        if (to.equals("SETTLED") && found.status().equals("OPEN")
                && settings.role(id, by).equals(SettingsRepository.CLERK)) {
            BigDecimal limit = settings.clerkLimit(id);
            BigDecimal now = repository.dueNow(id, found);
            if (limit != null && now != null && now.compareTo(limit) > 0) {
                to = "PENDING";
                event = "CLAIM_PENDING";
            }
        }
        // A period with a cap opens no claim belonging to it (by due-by) for a person who already has that many
        // OPEN or PENDING claims in it.
        if (to.equals("OPEN") && (found.status().equals("DRAFT") || found.status().equals("REVIEW"))) {
            Period in = periods.containing(id, LocalDate.parse(found.dueBy()));
            if (in != null && in.cap() != null && periods.openIn(id, found.personId(), in) >= in.cap()) {
                throw new BadRequestException("the person already has " + in.cap() + " open claims in the period " + in.name());
            }
        }
        // A claim whose due is above the review threshold in force on its date goes to REVIEW when opened, with
        // no number until a steward reviews it ('n/a' is above none).
        if (to.equals("OPEN") && found.status().equals("DRAFT")) {
            BigDecimal threshold = settings.reviewThreshold(id, LocalDate.parse(found.date()));
            BigDecimal now = repository.dueNow(id, found);
            if (threshold != null && now != null && now.compareTo(threshold) > 0) {
                to = "REVIEW";
                event = "CLAIM_REVIEW";
            }
        }
        if (checkOnly) {
            return null;
        }
        Rollup.Snapshot before = rollup.take(id);
        if (!repository.changeStatus(id, cid, from, to)) {
            throw new BadRequestException("a " + found.status() + " claim cannot be moved to " + to);
        }
        if (voidReason != null) {
            repository.voidReason(id, cid, voidReason);
        }
        if (to.equals("REVIEW")) {
            repository.reviewDay(id, cid);
        }
        // Opening gives the sheet's next number; a rejected claim keeps its own; a voided claim keeps the number it had (never reused).
        String number = found.number();
        String openedBy = found.openedBy();
        if ((found.status().equals("DRAFT") || found.status().equals("REVIEW")) && to.equals("OPEN")) {
            number = repository.numbersPerPeriod(id)
                    ? repository.numberInPeriod(id, cid, LocalDate.parse(found.dueBy()))
                    : repository.number(id, cid, LocalDate.parse(found.dueBy()));
        }
        // Who opened it is recorded when it is opened, also when it went to REVIEW; a review keeps it.
        if (found.status().equals("DRAFT") && (to.equals("OPEN") || to.equals("REVIEW"))) {
            repository.openedBy(id, cid, by);
            openedBy = by;
        }
        // Settling (or approving) fixes the claim's due as it is now: the figure its last statement bound it to,
        // else its live due ('n/a' when its grade has no rate, which stays live), with its late charge frozen in.
        String due = null;
        String tail = event.equals("CLAIM_REJECTED") ? " reason=" + why : "";
        if (reversing) {
            tail = " reversed=" + (found.due() == null ? "n/a" : found.due());
        }
        if (voidReason != null) {
            tail += " reason=" + voidReason;
        }
        if (to.equals("SETTLED")) {
            BigDecimal now = repository.dueNow(id, found);
            repository.settleDue(id, cid, now);
            due = ClaimRepository.dueOf(now);
            tail = " due=" + (due == null ? "n/a" : due);
        }
        repository.log(id, cid, by);
        Claim changed = new Claim(found.id(), found.personId(), found.amount(), found.date(), found.note(), to,
                number, due, openedBy, found.stated(), found.statedDue(), found.corrects(), found.dueBy(),
                found.statedNa(), found.deferredUntil(), found.deferred(), found.proofs(), found.splitFrom(), found.lateEntry(), found.reminded(), found.ref(), found.tags(),
                voidReason != null ? voidReason : found.voidReason());
        if (found.status().equals("REVIEW") && to.equals("OPEN")) {
            audit.record("CLAIM_REVIEWED sheet=" + id + " by=" + by + " person=" + repository.personName(id, found.personId())
                    + " amount=" + changed.amount() + " date=" + changed.date());
        }
        audit.record(event + " sheet=" + id + " by=" + by + " person=" + repository.personName(id, found.personId())
                + " amount=" + changed.amount() + " date=" + changed.date() + Claim.auditNumber(number) + tail);
        // A settled (or approved) claim's watchers are told, after the claim's own line and before the rollup.
        if (event.equals("CLAIM_SETTLED") || event.equals("CLAIM_APPROVED")) {
            watches.notify(id, found.personId(), repository.personName(id, found.personId()), number, by, audit);
        }
        rollup.record(id, by, before);
        return changed;
    }

    /** A steward re-settles a SETTLED claim (not in a closed period): its due is frozen again at the figure it would
     *  have today as an open claim bound by no statement (today's rates, grades, floor and late charge). */
    private static Claim resettle(String id, long cid, String by, ClaimRepository repository, PeriodRepository periods,
            SettingsRepository settings, Audit audit, Rollup rollup, WatchRepository watches) throws BadRequestException {
        Claim found = repository.find(id, cid);
        if (found == null) {
            throw new BadRequestException("no such claim");
        }
        if (!settings.role(id, by).equals(SettingsRepository.STEWARD)) {
            throw new BadRequestException("only a steward can re-settle a claim");
        }
        if (!found.status().equals("SETTLED")) {
            throw new BadRequestException("only a SETTLED claim can be re-settled");
        }
        PeriodGuard.open(periods, id, LocalDate.parse(found.dueBy()));
        Claim live = new Claim(found.id(), found.personId(), found.amount(), found.date(), found.note(), "OPEN",
                found.number(), null, found.openedBy(), false, null, found.corrects(), found.dueBy(), false, null, false, found.proofs(), found.splitFrom(), found.lateEntry(), found.reminded(), found.ref(), found.tags(), found.voidReason());
        BigDecimal now = repository.dueNow(id, live);
        Rollup.Snapshot before = rollup.take(id);
        repository.settleDue(id, cid, now);
        repository.log(id, cid, by);
        String old = found.due() == null ? "n/a" : found.due();
        String due = ClaimRepository.dueOf(now);
        Claim changed = new Claim(found.id(), found.personId(), found.amount(), found.date(), found.note(),
                found.status(), found.number(), due, found.openedBy(), found.stated(), found.statedDue(),
                found.corrects(), found.dueBy(), found.statedNa(), found.deferredUntil(), found.deferred(), found.proofs(), found.splitFrom(), found.lateEntry(), found.reminded(), found.ref(), found.tags(), found.voidReason());
        audit.record("CLAIM_RESETTLED sheet=" + id + " by=" + by + " person=" + repository.personName(id, found.personId())
                + " amount=" + changed.amount() + " date=" + changed.date() + Claim.auditNumber(found.number())
                + " old=" + old + " new=" + (due == null ? "n/a" : due));
        watches.notify(id, found.personId(), repository.personName(id, found.personId()), found.number(), by, audit);
        rollup.record(id, by, before);
        return changed;
    }
}
