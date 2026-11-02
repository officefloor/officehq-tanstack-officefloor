package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.period.PeriodGuard;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.settings.SettingsRepository;

/** A claim's status change (open, settle, approve, reject or void), shared by the single and the bulk actions. Every
 *  check is made before anything is written, so a refusal writes nothing to the store or the audit. */
final class ClaimStatusChange {
    private ClaimStatusChange() {
    }

    static Claim apply(String id, long cid, String action, String by, ClaimRepository repository,
            PeriodRepository periods, SettingsRepository settings, Audit audit, Rollup rollup) throws BadRequestException {
        return apply(id, cid, action, null, by, repository, periods, settings, audit, rollup);
    }

    static Claim apply(String id, long cid, String action, String reason, String by, ClaimRepository repository,
            PeriodRepository periods, SettingsRepository settings, Audit audit, Rollup rollup) throws BadRequestException {
        if ("resettle".equals(action == null ? "" : action.trim())) {
            return resettle(id, cid, by, repository, periods, settings, audit, rollup);
        }
        String to;
        List<String> from;
        String event;
        switch (action == null ? "" : action.trim()) {
            case "open" -> { to = "OPEN"; from = List.of("DRAFT"); event = "CLAIM_OPENED"; }
            case "settle" -> { to = "SETTLED"; from = List.of("OPEN"); event = "CLAIM_SETTLED"; }
            case "approve" -> { to = "SETTLED"; from = List.of("PENDING"); event = "CLAIM_APPROVED"; }
            case "reject" -> { to = "OPEN"; from = List.of("PENDING"); event = "CLAIM_REJECTED"; }
            case "void" -> { to = "VOID"; from = List.of("DRAFT", "OPEN", "PENDING"); event = "CLAIM_VOIDED"; }
            default -> throw new BadRequestException("no such action");
        }
        Claim found = repository.find(id, cid);
        if (found == null) {
            throw new BadRequestException("no such claim");
        }
        if (!from.contains(found.status())) {
            throw new BadRequestException("a " + found.status() + " claim cannot be moved to " + to);
        }
        PeriodGuard.open(periods, id, LocalDate.parse(found.date()));
        // Four eyes: whoever opened a claim cannot settle it.
        if (to.equals("SETTLED") && found.status().equals("OPEN") && by.equals(found.openedBy())) {
            throw new BadRequestException("the user who opened a claim cannot settle it");
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
        String why = reason == null ? "" : reason.trim();
        if (event.equals("CLAIM_REJECTED") && why.isEmpty()) {
            throw new BadRequestException("give a reason to reject the claim");
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
        Rollup.Snapshot before = rollup.take(id);
        if (!repository.changeStatus(id, cid, from, to)) {
            throw new BadRequestException("a " + found.status() + " claim cannot be moved to " + to);
        }
        // Opening gives the sheet's next number; a rejected claim keeps its own; a voided claim keeps the number it had (never reused).
        String number = found.number();
        String openedBy = found.openedBy();
        if (found.status().equals("DRAFT") && to.equals("OPEN")) {
            number = repository.numbersPerPeriod(id)
                    ? repository.numberInPeriod(id, cid, LocalDate.parse(found.date()))
                    : repository.number(id, cid);
            repository.openedBy(id, cid, by);
            openedBy = by;
        }
        // Settling (or approving) fixes the claim's due as it is now: the figure its last statement bound it to
        // (a PENDING claim's is live), else its live due ('n/a' when its grade has no rate, which stays live), with its late charge frozen in.
        String due = null;
        String tail = event.equals("CLAIM_REJECTED") ? " reason=" + why : "";
        if (to.equals("SETTLED")) {
            BigDecimal now = repository.dueNow(id, found);
            repository.settleDue(id, cid, now);
            due = ClaimRepository.dueOf(now);
            tail = " due=" + (due == null ? "n/a" : due);
        }
        repository.log(id, cid, by);
        Claim changed = new Claim(found.id(), found.personId(), found.amount(), found.date(), found.note(), to,
                number, due, openedBy, found.stated(), found.statedDue(), found.corrects(), found.dueBy(),
                found.statedNa());
        audit.record(event + " sheet=" + id + " by=" + by + " person=" + repository.personName(id, found.personId())
                + " amount=" + changed.amount() + " date=" + changed.date() + Claim.auditNumber(number) + tail);
        rollup.record(id, by, before);
        return changed;
    }

    /** A steward re-settles a SETTLED claim (not in a closed period): its due is frozen again at the figure it would
     *  have today as an open claim bound by no statement (today's rates, grades, floor and late charge). */
    private static Claim resettle(String id, long cid, String by, ClaimRepository repository, PeriodRepository periods,
            SettingsRepository settings, Audit audit, Rollup rollup) throws BadRequestException {
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
        PeriodGuard.open(periods, id, LocalDate.parse(found.date()));
        Claim live = new Claim(found.id(), found.personId(), found.amount(), found.date(), found.note(), "OPEN",
                found.number(), null, found.openedBy(), false, null, found.corrects(), found.dueBy(), false);
        BigDecimal now = repository.dueNow(id, live);
        Rollup.Snapshot before = rollup.take(id);
        repository.settleDue(id, cid, now);
        repository.log(id, cid, by);
        String old = found.due() == null ? "n/a" : found.due();
        String due = ClaimRepository.dueOf(now);
        Claim changed = new Claim(found.id(), found.personId(), found.amount(), found.date(), found.note(),
                found.status(), found.number(), due, found.openedBy(), found.stated(), found.statedDue(),
                found.corrects(), found.dueBy(), found.statedNa());
        audit.record("CLAIM_RESETTLED sheet=" + id + " by=" + by + " person=" + repository.personName(id, found.personId())
                + " amount=" + changed.amount() + " date=" + changed.date() + Claim.auditNumber(found.number())
                + " old=" + old + " new=" + (due == null ? "n/a" : due));
        rollup.record(id, by, before);
        return changed;
    }
}
