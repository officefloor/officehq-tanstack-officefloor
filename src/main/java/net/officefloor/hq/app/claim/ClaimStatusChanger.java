package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.hq.app.Audit;
import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.period.Period;
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
import net.officefloor.hq.app.voidreason.VoidReasons;
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
    private final VoidReasons voidReasons;

    public ClaimStatusChanger(Claims claims, People people, Rates rates, Periods periods, Roles roles, Settings settings,
            SheetClock clock, Audit audit, Rollup rollup, ClaimLog log, Watches watches, SheetArchive archive,
            VoidReasons voidReasons) {
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
        this.voidReasons = voidReasons;
    }

    /** Applies the change to the claim, writing nothing when it is refused. */
    public void change(String sheet, String user, Claim claim, ClaimStatusChange requested)
            throws InvalidClaimStatusChangeException {
        change(sheet, user, claim, requested, null);
    }

    /** Applies the change to the claim (a reject gives its reason, a void the one chosen from the sheet's void reasons),
     *  writing nothing when it is refused. */
    public void change(String sheet, String user, Claim claim, ClaimStatusChange requested, String reason)
            throws InvalidClaimStatusChangeException {
        change(sheet, user, claim, requested, reason, true);
    }

    /** Settles the claim just split off another to part-settle it, as the settle button does (to PENDING when the
     *  clerk limit says so), save that the four-eyes rules and the proof rule were judged on the claim it was split
     *  off, so are not judged again on it (whose opener is the user); writes nothing when it is refused. */
    public void settlePart(String sheet, String user, Claim part) throws InvalidClaimStatusChangeException {
        change(sheet, user, part, ClaimStatusChange.SETTLE, null, false);
    }

    private void change(String sheet, String user, Claim claim, ClaimStatusChange requested, String reason,
            boolean ownRules) throws InvalidClaimStatusChangeException {
        requested = vet(sheet, user, claim, requested, reason, ownRules);
        String voidReason = requested == ClaimStatusChange.VOID && voidReasons.any(sheet) ? reason.trim() : null;
        Person person = people.find(sheet, claim.person());
        Rollup.Snapshot before = rollup.take(sheet);
        ClaimStatus next = claim.status().after(requested);
        // Conditional on the status read, so a concurrent change by another user is refused, not overwritten.
        if (next == null || !claims.changeStatus(sheet, claim.id(), claim.status(), next)) {
            throw new InvalidClaimStatusChangeException(
                    "A " + claim.status() + " claim does not allow " + requested.name().toLowerCase());
        }
        // Opening gives the next number (FY<yyyy>-<n>) of its fiscal year its sheet's tree shares, or, while the sheet numbers by period from on or
        // before its today, the next number in the claim's period (X for none); a voided claim keeps the number it
        // had (never reused).
        Claim changed = claim;
        if (requested == ClaimStatusChange.OPEN || requested == ClaimStatusChange.REVIEW) {
            changed = number(sheet, claim);
        }
        // Who opened it is the user who pressed open, also when it went to REVIEW first (not the reviewing steward).
        if (requested == ClaimStatusChange.OPEN || requested == ClaimStatusChange.HOLD_FOR_REVIEW) {
            claims.setOpenedBy(sheet, claim.id(), user);
        }
        String fields = changed.statusAuditFields(person.name());
        // Settling (or approving a PENDING claim) fixes the due at the person's grade rate in force on the claim's date (or the figure its
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
        if (voidReason != null) {
            voidReasons.voided(sheet, claim.id(), voidReason);
            fields += " reason=" + voidReason;
        }
        log.record(sheet, user, claim.id());
        // A review is written (naming the claim, before its number) then the open it makes, with its number.
        if (requested == ClaimStatusChange.REVIEW) {
            audit.record(requested.event() + " sheet=" + sheet + " by=" + user + " " + claim.statusAuditFields(person.name()));
            audit.record(ClaimStatusChange.OPEN.event() + " sheet=" + sheet + " by=" + user + " " + fields);
        } else {
            audit.record(requested.event() + " sheet=" + sheet + " by=" + user + " " + fields);
        }
        // Settling or approving tells each user watching the person, before the rollup lines.
        if (requested == ClaimStatusChange.SETTLE || requested == ClaimStatusChange.APPROVE) {
            watches.notify(sheet, user, person.id(), person.name(), changed.shownNumber());
        }
        rollup.record(sheet, user, before);
    }

    /** Refuses the change the user may not make to the claim as it is now, writing nothing; otherwise gives the change
     *  it makes (a clerk's settle above the clerk limit is a move to PENDING). */
    public ClaimStatusChange vet(String sheet, String user, Claim claim, ClaimStatusChange requested, String reason)
            throws InvalidClaimStatusChangeException {
        return vet(sheet, user, claim, requested, reason, true);
    }

    /** As {@link #vet}, judging the four-eyes rules and the proof rule on the claim only when ownRules. */
    private ClaimStatusChange vet(String sheet, String user, Claim claim, ClaimStatusChange requested, String reason,
            boolean ownRules) throws InvalidClaimStatusChangeException {
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
        // While its person's flag is in force (the sheet's today on or before its until-date) only a steward opens a claim.
        if (requested == ClaimStatusChange.OPEN && !Role.STEWARD.equals(roles.of(sheet, user))) {
            Person flagged = people.find(sheet, claim.person());
            if (flagged != null && flagged.flagged()) {
                throw new InvalidClaimStatusChangeException(
                        "Only a steward may open a claim of " + flagged.name() + " while flagged: " + flagged.flagReason());
            }
        }
        // Only a steward reviews a REVIEW claim, opening it.
        if (requested == ClaimStatusChange.REVIEW && !Role.STEWARD.equals(roles.of(sheet, user))) {
            throw new InvalidClaimStatusChangeException("Only a steward reviews a claim");
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
        // A DELETED claim is restored only on the sheet's today it was deleted (its status log's last DELETED date).
        if (requested == ClaimStatusChange.RESTORE && claim.status() == ClaimStatus.DELETED
                && !clock.today(sheet).equals(claim.deletedOn())) {
            throw new InvalidClaimStatusChangeException("A deleted claim may be restored only on the day it was deleted");
        }
        if (requested == ClaimStatusChange.SETTLE && claim.deferred()) {
            throw new InvalidClaimStatusChangeException("A deferred claim cannot be settled until its date");
        }
        // Four eyes: the user who opened a claim may not settle it, nor approve it.
        if (ownRules && (requested == ClaimStatusChange.SETTLE || requested == ClaimStatusChange.APPROVE)
                && user.equals(claim.openedBy())) {
            throw new InvalidClaimStatusChangeException("The user who opened a claim cannot settle or approve it");
        }
        // Nor may the user who last edited it.
        if (ownRules && (requested == ClaimStatusChange.SETTLE || requested == ClaimStatusChange.APPROVE)
                && user.equals(claims.editedBy(sheet, claim.id()))) {
            throw new InvalidClaimStatusChangeException("The user who last edited a claim cannot settle or approve it");
        }
        // Only a steward approves or rejects a PENDING claim; a reject gives a reason.
        if ((requested == ClaimStatusChange.APPROVE || requested == ClaimStatusChange.REJECT)
                && !Role.STEWARD.equals(roles.of(sheet, user))) {
            throw new InvalidClaimStatusChangeException("Only a steward approves or rejects a claim");
        }
        if (requested == ClaimStatusChange.REJECT && (reason == null || reason.isBlank())) {
            throw new InvalidClaimStatusChangeException("A reject needs a reason");
        }
        // While the sheet has void reasons, a void needs one chosen from them (with none, a void takes no reason).
        if (requested == ClaimStatusChange.VOID && voidReasons.any(sheet)) {
            String voidReason = reason == null ? "" : reason.trim();
            if (voidReason.isEmpty() || !voidReasons.has(sheet, voidReason)) {
                throw new InvalidClaimStatusChangeException("A void needs a reason from the sheet's void reasons");
            }
        }
        Person person = people.find(sheet, claim.person());
        // A claim whose due is above the proof threshold in force on its date (its person's grade's then when it has
        // one, else the sheet's) is settled or approved only once it has
        // a proof (a due of n/a is above no threshold); checked before the clerk limit, so it never goes to PENDING.
        if (ownRules && (requested == ClaimStatusChange.SETTLE || requested == ClaimStatusChange.APPROVE)) {
            BigDecimal threshold = settings.proofThreshold(sheet, person.gradeOn(claim.date()),
                    LocalDate.parse(claim.date()));
            // While the sheet has proof kinds, it needs one proof of each of them, not just one.
            List<String> kinds = settings.proofKinds(sheet);
            boolean proven = kinds.isEmpty() ? claim.proofs() > 0 : kinds.stream().allMatch((k) -> claim.proofsOf(k) > 0);
            if (threshold != null && !proven) {
                BigDecimal due = claim.due(rates.list(sheet), person.gradeOn(claim.date()), settings.floor(sheet),
                        settings.late(sheet, clock.today(sheet)));
                if (due != null && due.compareTo(threshold) > 0) {
                    throw new InvalidClaimStatusChangeException(
                            "A claim whose due is above the proof threshold needs a proof (one of each of the sheet's proof kinds) before it is settled or approved");
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
        // A clerk settles only a due at or below the clerk limit in force on the sheet's today (its person's grade's
        // when it has one, else the sheet's; a due of n/a is within any limit); above
        // it the claim goes to PENDING for a steward to approve. A steward settles anything.
        if (requested == ClaimStatusChange.SETTLE && claim.status() == ClaimStatus.OPEN
                && Role.CLERK.equals(roles.of(sheet, user))) {
            BigDecimal limit = settings.clerkLimit(sheet, person.gradeOn(claim.date()));
            BigDecimal due = claim.due(rates.list(sheet), person.gradeOn(claim.date()), settings.floor(sheet),
                    settings.late(sheet, clock.today(sheet)));
            if (limit != null && due != null && due.compareTo(limit) > 0) {
                requested = ClaimStatusChange.PEND;
            }
        }
        // A claim whose due is above the review threshold in force on its date goes to REVIEW when opened, for a
        // steward to review (a due of n/a is above no threshold).
        if (requested == ClaimStatusChange.OPEN && claim.status() == ClaimStatus.DRAFT) {
            BigDecimal threshold = settings.reviewThreshold(sheet, LocalDate.parse(claim.date()));
            if (threshold != null) {
                BigDecimal due = claim.due(rates.list(sheet), person.gradeOn(claim.date()), settings.floor(sheet),
                        settings.late(sheet, clock.today(sheet)));
                if (due != null && due.compareTo(threshold) > 0) {
                    requested = ClaimStatusChange.HOLD_FOR_REVIEW;
                }
            }
        }
        if (claim.status().after(requested) == null) {
            throw new InvalidClaimStatusChangeException(
                    "A " + claim.status() + " claim does not allow " + requested.name().toLowerCase());
        }
        // A period with a cap lets each person have at most that many OPEN or PENDING claims due inside it.
        if (requested == ClaimStatusChange.OPEN || requested == ClaimStatusChange.REVIEW) {
            Period period = periods.on(sheet, LocalDate.parse(claim.dueBy()));
            if (period != null && period.cap() != null) {
                long held = claims.list(sheet).stream()
                        .filter((c) -> c.person() == claim.person() && c.id() != claim.id())
                        .filter((c) -> c.status() == ClaimStatus.OPEN || c.status() == ClaimStatus.PENDING)
                        .filter((c) -> c.dueBy().compareTo(period.from()) >= 0 && c.dueBy().compareTo(period.to()) <= 0)
                        .count();
                if (held >= period.cap()) {
                    throw new InvalidClaimStatusChangeException("A person may have at most " + period.cap()
                            + " open claims in period " + period.name());
                }
            }
        }
        return requested;
    }

    /**
     * Gives an opening claim the next number (FY<yyyy>-<n>) of its due-by's fiscal year in the sequence its sheet shares with its tree, or, while the sheet numbers by period from on or before its
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
        // Sheet-wide numbering restarts each fiscal year (of the due-by, the date that places it in a period):
        // FY<yyyy>-<n>, kept as the number it shows.
        LocalDate dueBy = LocalDate.parse(claim.dueBy());
        int start = settings.fiscalStartMonth(sheet);
        String fy = "FY" + (dueBy.getMonthValue() >= start ? dueBy.getYear() : dueBy.getYear() - 1);
        Claim numbered = claim.withPeriodNumber(fy + "-" + claims.nextFiscalYearNumber(sheet, settings.wholeTree(sheet), fy));
        claims.setPeriodNumber(sheet, claim.id(), numbered.periodNumber());
        return numbered;
    }
}
