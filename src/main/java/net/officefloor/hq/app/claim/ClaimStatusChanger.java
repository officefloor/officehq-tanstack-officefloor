package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.hq.app.Audit;
import java.time.LocalDate;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.Roles;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.SheetArchive;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.hq.app.watch.Watches;
import org.springframework.stereotype.Service;

/** Moves a claim to the status a change takes it to, refusing a change its status does not allow; shared by a
 *  claim's own status buttons and the bulk actions. */
@Service
public class ClaimStatusChanger {

    /** How many days after its settlement a steward may still void a SETTLED claim. */
    public static final int VOID_SETTLED_DAYS = 7;

    private final Claims claims;
    private final People people;
    private final Rates rates;
    private final Periods periods;
    private final Roles roles;
    private final Settings settings;
    private final SheetClock clock;
    private final Audit audit;
    private final Rollup rollup;
    private final ClaimLog log;
    private final Watches watches;
    private final SheetArchive archive;

    public ClaimStatusChanger(Claims claims, People people, Rates rates, Periods periods, Roles roles, Settings settings,
            SheetClock clock, Audit audit, Rollup rollup, ClaimLog log, Watches watches, SheetArchive archive) {
        this.claims = claims;
        this.people = people;
        this.rates = rates;
        this.periods = periods;
        this.roles = roles;
        this.settings = settings;
        this.clock = clock;
        this.audit = audit;
        this.rollup = rollup;
        this.log = log;
        this.watches = watches;
        this.archive = archive;
    }

    /** Applies the change to the claim, writing nothing when it is refused. */
    public void change(String sheet, String user, Claim claim, ClaimStatusChange requested)
            throws InvalidClaimStatusChangeException {
        change(sheet, user, claim, requested, null);
    }

    /** Applies the change to the claim (a reject gives its reason), writing nothing when it is refused. */
    public void change(String sheet, String user, Claim claim, ClaimStatusChange requested, String reason)
            throws InvalidClaimStatusChangeException {
        // A viewer changes nothing.
        if (Role.VIEWER.equals(roles.of(sheet, user))) {
            throw new InvalidClaimStatusChangeException("A viewer cannot change a claim");
        }
        if (archive.archived(sheet)) {
            throw new InvalidClaimStatusChangeException("An archived sheet cannot change");
        }
        // The permission matrix says which roles open, settle and void a claim.
        if ((requested == ClaimStatusChange.OPEN || requested == ClaimStatusChange.SETTLE || requested == ClaimStatusChange.VOID)
                && !settings.permits(sheet, requested.name().toLowerCase(), roles.of(sheet, user))) {
            throw new InvalidClaimStatusChangeException("Your role may not " + requested.name().toLowerCase() + " a claim");
        }
        if (periods.closedOn(sheet, LocalDate.parse(claim.dueBy()))) {
            throw new InvalidClaimStatusChangeException("A claim due inside a closed period cannot change status");
        }
        // A steward voids a SETTLED claim only within 7 days of its settlement (its status log's last SETTLED date).
        if (requested == ClaimStatusChange.VOID && claim.status() == ClaimStatus.SETTLED) {
            if (!Role.STEWARD.equals(roles.of(sheet, user))) {
                throw new InvalidClaimStatusChangeException("Only a steward voids a settled claim");
            }
            LocalDate settled = claim.settledOn();
            LocalDate today = clock.today(sheet);
            if (settled == null || today.isBefore(settled) || today.isAfter(settled.plusDays(VOID_SETTLED_DAYS))) {
                throw new InvalidClaimStatusChangeException(
                        "A settled claim may be voided only within " + VOID_SETTLED_DAYS + " days of its settlement");
            }
        }
        if (requested == ClaimStatusChange.SETTLE && claim.deferred()) {
            throw new InvalidClaimStatusChangeException("A deferred claim cannot be settled until its date");
        }
        // Four eyes: the user who opened a claim may not settle it, nor approve it.
        if ((requested == ClaimStatusChange.SETTLE || requested == ClaimStatusChange.APPROVE)
                && user.equals(claim.openedBy())) {
            throw new InvalidClaimStatusChangeException("The user who opened a claim cannot settle or approve it");
        }
        // Only a steward approves or rejects a PENDING claim; a reject gives a reason.
        if ((requested == ClaimStatusChange.APPROVE || requested == ClaimStatusChange.REJECT)
                && !Role.STEWARD.equals(roles.of(sheet, user))) {
            throw new InvalidClaimStatusChangeException("Only a steward approves or rejects a claim");
        }
        if (requested == ClaimStatusChange.REJECT && (reason == null || reason.isBlank())) {
            throw new InvalidClaimStatusChangeException("A reject needs a reason");
        }
        Person person = people.find(sheet, claim.person());
        // A claim whose due is above the proof threshold in force on its date is settled or approved only once it has
        // a proof (a due of n/a is above no threshold); checked before the clerk limit, so it never goes to PENDING.
        if (requested == ClaimStatusChange.SETTLE || requested == ClaimStatusChange.APPROVE) {
            BigDecimal threshold = settings.proofThreshold(sheet, LocalDate.parse(claim.date()));
            if (threshold != null && claim.proofs() == 0) {
                BigDecimal due = claim.due(rates.list(sheet), person.gradeOn(claim.date()), settings.floor(sheet),
                        settings.late(sheet, clock.today(sheet)));
                if (due != null && due.compareTo(threshold) > 0) {
                    throw new InvalidClaimStatusChangeException(
                            "A claim whose due is above the proof threshold needs a proof before it is settled or approved");
                }
            }
        }
        // A claim whose due is above the note threshold in force on its date is opened only with a note (a due of n/a
        // is above no threshold).
        if (requested == ClaimStatusChange.OPEN && (claim.note() == null || claim.note().isBlank())) {
            BigDecimal threshold = settings.noteThreshold(sheet, LocalDate.parse(claim.date()));
            if (threshold != null) {
                BigDecimal due = claim.due(rates.list(sheet), person.gradeOn(claim.date()), settings.floor(sheet),
                        settings.late(sheet, clock.today(sheet)));
                if (due != null && due.compareTo(threshold) > 0) {
                    throw new InvalidClaimStatusChangeException(
                            "A claim whose due is above the note threshold needs a note before it is opened");
                }
            }
        }
        // A clerk settles only a due at or below the sheet's clerk limit (a due of n/a is within any limit); above
        // it the claim goes to PENDING for a steward to approve. A steward settles anything.
        if (requested == ClaimStatusChange.SETTLE && claim.status() == ClaimStatus.OPEN
                && Role.CLERK.equals(roles.of(sheet, user))) {
            BigDecimal limit = settings.clerkLimit(sheet);
            BigDecimal due = claim.due(rates.list(sheet), person.gradeOn(claim.date()), settings.floor(sheet),
                    settings.late(sheet, clock.today(sheet)));
            if (limit != null && due != null && due.compareTo(limit) > 0) {
                requested = ClaimStatusChange.PEND;
            }
        }
        Rollup.Snapshot before = rollup.take(sheet);
        ClaimStatus next = claim.status().after(requested);
        // Conditional on the status read, so a concurrent change by another user is refused, not overwritten.
        if (next == null || !claims.changeStatus(sheet, claim.id(), claim.status(), next)) {
            throw new InvalidClaimStatusChangeException(
                    "A " + claim.status() + " claim does not allow " + requested.name().toLowerCase());
        }
        // Opening gives the sheet's next number, or, while the sheet numbers by period from on or before its today,
        // the next number in the claim's period (X for none); a voided claim keeps the number it had (never reused).
        Claim changed = claim;
        if (requested == ClaimStatusChange.OPEN) {
            changed = number(sheet, claim);
            claims.setOpenedBy(sheet, claim.id(), user);
        }
        String fields = changed.statusAuditFields(person.name());
        // Settling (or approving a PENDING claim, its live figure then) fixes the due at the person's grade rate in force on the claim's date (or the figure its
        // statement bound), with its late charge frozen in; with no rate there is nothing to fix, so it stays live.
        if (requested == ClaimStatusChange.SETTLE || requested == ClaimStatusChange.APPROVE) {
            BigDecimal due = claim.due(rates.list(sheet), person.gradeOn(claim.date()), settings.floor(sheet),
                    settings.late(sheet, clock.today(sheet)));
            claims.setSettledDue(sheet, claim.id(), due);
            fields += " due=" + (due == null ? "n/a" : due.toPlainString());
        }
        // Voiding a SETTLED claim reverses its frozen figure out of every total.
        if (requested == ClaimStatusChange.VOID && claim.status() == ClaimStatus.SETTLED) {
            fields += " reversed=" + (claim.settledDue() == null ? "n/a" : claim.settledDue());
        }
        if (requested == ClaimStatusChange.REJECT) {
            fields += " reason=" + reason.trim();
        }
        log.record(sheet, user, claim.id());
        audit.record(requested.event() + " sheet=" + sheet + " by=" + user + " " + fields);
        // Settling or approving tells each user watching the person, before the rollup lines.
        if (requested == ClaimStatusChange.SETTLE || requested == ClaimStatusChange.APPROVE) {
            watches.notify(sheet, user, person.id(), person.name(), changed.shownNumber());
        }
        rollup.record(sheet, user, before);
    }

    /**
     * Gives an opening claim the sheet's next number, or, while the sheet numbers by period from on or before its
     * today, the next number in the claim's period (X for none); returns the claim so numbered.
     */
    public Claim number(String sheet, Claim claim) {
        LocalDate from = settings.periodNumberingFrom(sheet);
        if (from != null && !clock.today(sheet).isBefore(from)) {
            String period = periods.nameOn(sheet, LocalDate.parse(claim.dueBy()));
            String key = period == null ? "" : period;
            Claim numbered = claim.withPeriodNumber((period == null ? "X" : period) + "-" + claims.nextPeriodNumber(sheet, key));
            claims.setPeriodNumber(sheet, claim.id(), numbered.periodNumber());
            return numbered;
        }
        Claim numbered = claim.withNumber(claims.nextNumber(sheet));
        claims.setNumber(sheet, claim.id(), numbered.number());
        return numbered;
    }
}
