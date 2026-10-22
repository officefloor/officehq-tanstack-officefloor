package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.setting.Permissions;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.watch.WatchRepository;
import net.officefloor.hq.app.statement.Dues;
import net.officefloor.hq.app.statement.LateCharge;

/** The rules of a claim's status change and its audit line, shared by a single change ({@link SaveClaimStatus},
 *  {@link AuditClaimStatus}) and a bulk settle ({@link BulkSettleClaims}). */
final class ClaimStatusChange {
    private ClaimStatusChange() {
    }

    /** How many days after its settlement (by the sheet's today, as its status log records it) a steward may still
     *  void a SETTLED claim. */
    private static final int VOID_SETTLED_DAYS = 7;

    /** Move the claim to its new status as {@link SaveClaimStatus} describes, adding it to its status log: the claim
     *  as stored now; throws (having written nothing) when the change is not allowed. */
    static ClaimResponse save(ClaimStatusInput input, Claim loaded, ClaimRepository repository,
            PersonRepository people, RateRepository rates, RoleRepository roles, SettingRepository settings,
            SheetRepository sheets, ClaimLogRepository log)
            throws ClaimStatusNotAllowedException {
        ClaimStatus to = input.to();
        if (Permissions.ACTIONS.contains(input.action()) && !settings.permissions(loaded.sheetId())
                .allows(input.action(), roles.of(loaded.sheetId(), input.user()))) {
            throw new ClaimStatusNotAllowedException("Your role may not " + input.action() + " a claim on this sheet.");
        }
        boolean review = input.action().equals("approve") || input.action().equals("reject");
        if (review) {
            if (loaded.status() != ClaimStatus.PENDING) {
                throw new ClaimStatusNotAllowedException("Only a PENDING claim can be " + input.action()
                        + "d; this one is " + loaded.status() + ".");
            }
            if (!Role.STEWARD.equals(roles.of(loaded.sheetId(), input.user()))) {
                throw new ClaimStatusNotAllowedException("Only a steward can " + input.action() + " a PENDING claim.");
            }
            if (input.action().equals("approve") && input.user().equals(loaded.openedBy())) {
                throw new ClaimStatusNotAllowedException("The user who opened a claim cannot approve it.");
            }
        } else if (input.action().equals("resettle")) {
            if (loaded.status() != ClaimStatus.SETTLED) {
                throw new ClaimStatusNotAllowedException("Only a SETTLED claim can be re-settled; this one is "
                        + loaded.status() + ".");
            }
            if (!Role.STEWARD.equals(roles.of(loaded.sheetId(), input.user()))) {
                throw new ClaimStatusNotAllowedException("Only a steward can re-settle a claim.");
            }
        } else if (input.action().equals("void") && loaded.status() == ClaimStatus.SETTLED) {
            if (!Role.STEWARD.equals(roles.of(loaded.sheetId(), input.user()))) {
                throw new ClaimStatusNotAllowedException("Only a steward can void a SETTLED claim.");
            }
            LocalDate settledOn = log.lastOn(loaded.sheetId(), loaded.id(), ClaimStatus.SETTLED);
            LocalDate today = sheets.today(loaded.sheetId());
            if (settledOn == null || today == null || today.isAfter(settledOn.plusDays(VOID_SETTLED_DAYS))) {
                throw new ClaimStatusNotAllowedException("A SETTLED claim can only be voided within "
                        + VOID_SETTLED_DAYS + " days of its settlement.");
            }
        } else if (!loaded.status().canBecome(to)) {
            throw new ClaimStatusNotAllowedException(loaded.status(), to);
        }
        if (input.action().equals("settle") && loaded.deferredOn(sheets.today(loaded.sheetId()))) {
            throw new ClaimStatusNotAllowedException("This claim is deferred until " + loaded.deferredUntil()
                    + "; it cannot be settled before then.");
        }
        if (input.action().equals("settle") && input.user().equals(loaded.openedBy())) {
            throw new ClaimStatusNotAllowedException("The user who opened a claim cannot settle it.");
        }
        if ((input.action().equals("settle") || input.action().equals("approve")) && loaded.proofs() == 0) {
            BigDecimal threshold = settings.proofThreshold(loaded.sheetId()).on(loaded.date());
            BigDecimal due = dueNow(loaded, repository, people, rates, settings, sheets);
            if (threshold != null && due != null && due.compareTo(threshold) > 0) {
                throw new ClaimStatusNotAllowedException("This claim's due " + due + " is above the proof threshold "
                        + threshold + "; add a proof before it is " + input.action() + "d.");
            }
        }
        if (input.action().equals("open")) {
            String refused = noteRequired(loaded, repository, people, rates, settings, sheets);
            if (refused != null) {
                throw new ClaimStatusNotAllowedException(refused);
            }
        }
        if (input.action().equals("settle") && Role.CLERK.equals(roles.of(loaded.sheetId(), input.user()))) {
            BigDecimal limit = settings.clerkLimit(loaded.sheetId());
            BigDecimal due = dueNow(loaded, repository, people, rates, settings, sheets);
            if (limit != null && due != null && due.compareTo(limit) > 0) {
                to = ClaimStatus.PENDING;
            }
        }
        boolean opening = input.action().equals("open");
        String openedBy = opening ? input.user() : loaded.openedBy();
        String number = opening ? repository.nextNumber(loaded.sheetId(), loaded.dueBy()) : loaded.number();
        BigDecimal settledDue = input.action().equals("resettle")
                ? dueNow(asOpenUnstated(loaded), repository, people, rates, settings, sheets)
                : to == ClaimStatus.SETTLED ? dueNow(loaded, repository, people, rates, settings, sheets)
                : to == ClaimStatus.VOID && loaded.status() == ClaimStatus.SETTLED ? loaded.settledDue() : null;
        if (!repository.changeStatus(loaded, to, number, settledDue, openedBy)) {
            // the transaction rolls back, so a number taken above is not used up
            throw new ClaimStatusNotAllowedException(loaded.status(), to);
        }
        log.record(loaded, input.user());
        return ClaimResponse.from(new Claim(loaded.id(), loaded.sheetId(), loaded.personId(), loaded.amount(),
                loaded.date(), loaded.note(), to, number, settledDue, openedBy, loaded.stated(),
                loaded.statedDue(), loaded.corrects(), loaded.dueBy(), loaded.deferredUntil(), loaded.proofs(), loaded.splitFrom(), loaded.lateEntry(), loaded.reminded()));
    }

    /** Record the stored status change in the audit file as {@link AuditClaimStatus} describes. */
    static void audit(ClaimStatusInput input, Claim loaded, ClaimResponse saved, PersonRepository people,
            WatchRepository watches, Audit audit) {
        var person = people.find(loaded.sheetId(), loaded.personId());
        String event = switch (input.action()) {
            case "open" -> "CLAIM_OPENED";
            case "settle" -> "PENDING".equals(saved.status()) ? "CLAIM_PENDING" : "CLAIM_SETTLED";
            case "approve" -> "CLAIM_APPROVED";
            case "reject" -> "CLAIM_REJECTED";
            case "resettle" -> "CLAIM_RESETTLED";
            case "void" -> "CLAIM_VOIDED";
            default -> throw new IllegalStateException("Unknown claim action: " + input.action() + ".");
        };
        boolean resettled = input.action().equals("resettle");
        boolean reversed = input.action().equals("void") && loaded.status() == ClaimStatus.SETTLED;
        boolean settled = "SETTLED".equals(saved.status()) && !resettled;
        String number = saved.number() != null ? saved.number() : loaded.number();
        audit.record(event + " sheet=" + loaded.sheetId() + " by=" + input.user()
                + " person=" + person.name() + " amount=" + saved.amount() + " date=" + saved.date()
                + (number != null ? " number=" + number : "")
                + (settled ? " due=" + (saved.settledDue() != null ? saved.settledDue() : "n/a") : "")
                + (input.reason() != null ? " reason=" + input.reason() : "")
                + (resettled ? " old=" + (loaded.settledDue() != null ? loaded.settledDue() : "n/a")
                        + " new=" + (saved.settledDue() != null ? saved.settledDue() : "n/a") : "")
                + (reversed ? " reversed=" + (loaded.settledDue() != null ? loaded.settledDue() : "n/a") : ""));
        if (event.equals("CLAIM_SETTLED") || event.equals("CLAIM_APPROVED") || event.equals("CLAIM_RESETTLED")) {
            for (String watcher : watches.watchers(loaded.sheetId(), loaded.personId())) {
                audit.record("NOTIFY sheet=" + loaded.sheetId() + " by=" + input.user() + " user=" + watcher
                        + " person=" + person.name() + " number=" + number);
            }
        }
    }

    /** The claim as an OPEN claim bound by no statement and not yet settled, for re-settling it at today's
     *  figure. */
    private static Claim asOpenUnstated(Claim c) {
        return new Claim(c.id(), c.sheetId(), c.personId(), c.amount(), c.date(), c.note(), ClaimStatus.OPEN,
                c.number(), null, c.openedBy(), false, null, c.corrects(), c.dueBy(), null, c.proofs(), c.splitFrom(), c.lateEntry(), c.reminded());
    }

    /** The claim's due now, as its row shows it: the figure a statement bound it to, else live (amount times its
     *  person's grade rate in force on the claim's date, divided by 100, rounded to two decimals as the claim rounds), with
     *  the late charge on it as of the sheet's today added, at least the sheet's floor in force on its date; null
     *  when it has no figure. */
    /** Why the claim cannot be opened as it is: its note is empty and its due is above the sheet's note threshold
     *  in force on its date; null when it may be opened. */
    static String noteRequired(Claim claim, ClaimRepository claims, PersonRepository people, RateRepository rates,
            SettingRepository settings, SheetRepository sheets) {
        if (claim.note() != null && !claim.note().isBlank()) {
            return null;
        }
        BigDecimal threshold = settings.noteThreshold(claim.sheetId()).on(claim.date());
        if (threshold == null) {
            return null;
        }
        BigDecimal due = dueNow(claim, claims, people, rates, settings, sheets);
        if (due == null || due.compareTo(threshold) <= 0) {
            return null;
        }
        return "This claim's due " + due + " is above the note threshold " + threshold
                + "; give it a note before it is opened.";
    }

    private static BigDecimal dueNow(Claim claim, ClaimRepository claims, PersonRepository people, RateRepository rates,
            SettingRepository settings, SheetRepository sheets) {
        var person = people.find(claim.sheetId(), claim.personId());
        return Dues.shown(claim, person.grades(), rates, settings.floor(claim.sheetId()),
                LateCharge.of(claim.sheetId(), settings, sheets, claims));
    }
}
