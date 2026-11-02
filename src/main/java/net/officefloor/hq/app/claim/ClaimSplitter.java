package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.holiday.Holidays;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.Roles;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.SheetClock;
import org.springframework.stereotype.Service;

/**
 * Splits an amount (more than 0.00 and less than the claim's) off an OPEN claim that is not deferred nor due inside a
 * closed period; only a role that may open a claim may, as the new claim is theirs to open. The claim keeps the rest
 * and loses any figure a statement bound it to; the new OPEN claim has the same date, note and tags, no number (it is given at settlement),
 * the user as its opener, a due-by fixed now from the terms in force today, and the claim's number as split-from.
 * Shared by a claim's Split and Settle part buttons.
 */
@Service
public class ClaimSplitter {

    private final Claims claims;
    private final People people;
    private final Periods periods;
    private final Roles roles;
    private final Settings settings;
    private final Holidays holidays;
    private final SheetClock clock;
    private final ClaimStatusChanger changer;
    private final ClaimLog log;
    private final Audit audit;
    private final Rollup rollup;

    public ClaimSplitter(Claims claims, People people, Periods periods, Roles roles, Settings settings, Holidays holidays,
            SheetClock clock, ClaimStatusChanger changer, ClaimLog log, Audit audit, Rollup rollup) {
        this.claims = claims;
        this.people = people;
        this.periods = periods;
        this.roles = roles;
        this.settings = settings;
        this.holidays = holidays;
        this.clock = clock;
        this.changer = changer;
        this.log = log;
        this.audit = audit;
        this.rollup = rollup;
    }

    /** Splits the amount off the claim, writing nothing when it is refused; returns the new OPEN claim. */
    public Claim split(String sheet, String user, Claim claim, BigDecimal amount) throws InvalidClaimStatusChangeException {
        if (Role.VIEWER.equals(roles.of(sheet, user)) || !settings.permits(sheet, "open", roles.of(sheet, user))) {
            throw new InvalidClaimStatusChangeException("Your role may not open a claim, so may not split one");
        }
        if (claim.status() != ClaimStatus.OPEN) {
            throw new InvalidClaimStatusChangeException("A " + claim.status() + " claim cannot be split");
        }
        if (claim.deferred()) {
            throw new InvalidClaimStatusChangeException("A deferred claim cannot be split");
        }
        // A split is judged by the claim split alone: by the period its due-by falls in.
        LocalDate date = LocalDate.parse(claim.date());
        if (periods.closedOn(sheet, LocalDate.parse(claim.dueBy()))) {
            throw new InvalidClaimStatusChangeException("A claim due inside a closed period cannot be split");
        }
        BigDecimal was = new BigDecimal(claim.amount());
        if (amount.signum() <= 0 || amount.compareTo(was) >= 0) {
            throw new InvalidClaimStatusChangeException("A split amount is more than 0.00 and less than the claim's amount");
        }
        Person person = people.find(sheet, claim.person());
        Rollup.Snapshot before = rollup.take(sheet);
        BigDecimal rest = was.subtract(amount);
        // Conditional on the amount and status read, so a concurrent change by another user is refused, not overwritten.
        if (!claims.split(sheet, claim.id(), was, rest)) {
            throw new InvalidClaimStatusChangeException("The claim changed since it was read");
        }
        LocalDate today = clock.today(sheet);
        LocalDate dueBy = person.dueBy(sheet, date, today, settings, holidays);
        long id = claims.addSplit(sheet, person.id(), amount, date, claim.note(), claim.tags(), dueBy,
                person.termsOn(sheet, today, settings), claim.shownNumber(), user);
        // Judged by the claim split alone: the new claim is a late entry when that claim is one.
        if (claim.lateEntry()) {
            claims.setLateEntry(sheet, id);
        }
        // The new claim is OPEN, so has no number until it is settled.
        Claim opened = claims.find(sheet, id);
        log.record(sheet, user, id);
        audit.record("CLAIM_SPLIT sheet=" + sheet + " by=" + user + " "
                + claim.withAmount(rest.toPlainString()).statusAuditFields(person.name()) + " into=" + (opened.shownNumber() == null ? "" : opened.shownNumber()));
        audit.record(ClaimStatusChange.OPEN.event() + " sheet=" + sheet + " by=" + user + " "
                + opened.statusAuditFields(person.name()));
        rollup.record(sheet, user, before);
        return claims.find(sheet, id);
    }
}
