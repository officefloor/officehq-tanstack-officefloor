package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.access.LateCharge;
import net.officefloor.hq.app.access.Role;
import net.officefloor.hq.app.access.RoleRepository;
import net.officefloor.hq.app.access.SettingRepository;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Moves the claim to the asked status, refusing a change its current status does not allow,
 * refusing to let the user who opened it settle it, and refusing a clerk a settle whose due is above the sheet's
 * clerk limit (a due of n/a is within any limit; a steward settles anything). Opening records who opened it; a voided claim keeps its number. Settling
 * fixes the claim's due at the rate in force on the claim's date of its person's grade on that date (at least the sheet's floor) — or, while a statement binds it, at
 * the figure it was bound to — with its late charge as of the sheet's today frozen in on top; without a figure its due stays live.
 */
public class ApplyClaimStatus {
    public void apply(@Val ClaimStatus status, @Val Claim claim, ClaimRepository repository,
            PersonRepository people, RateRepository rates, RoleRepository roles, SettingRepository settings, SheetRepository sheets,
            PeriodRepository periods, @Val ChangedBy by, Out<ClaimNumber> audited) throws BadRequestException {
        ClaimStatus from = claim.getStatus();
        if (!from.canBecome(status)) {
            throw new BadRequestException("A " + from + " claim cannot become " + status);
        }
        if (status == ClaimStatus.SETTLED && by.name().equals(claim.getOpenedBy())) {
            throw new BadRequestException("The user who opened a claim cannot settle it");
        }
        LateCharge late = settings.lateCharge(claim.getSheetId(), sheets.today(claim.getSheetId()));
        BigDecimal due = status == ClaimStatus.SETTLED ? late.due(claim, dueNow(claim, people, rates, settings, late)) : null;
        if (status == ClaimStatus.SETTLED && due != null
                && !Role.STEWARD.equals(roles.roleOf(claim.getSheetId(), by.name()))) {
            BigDecimal limit = settings.clerkLimit(claim.getSheetId());
            if (limit != null && due.compareTo(limit) > 0) {
                throw new BadRequestException("A clerk can settle a claim only when its due is at or below the clerk limit ("
                        + limit.toPlainString() + ")");
            }
        }
        claim.setStatus(status);
        if (status == ClaimStatus.OPEN) {
            claim.setOpenedBy(by.name());
        }
        if (status == ClaimStatus.SETTLED) {
            claim.setSettledDue(due);
        }
        if (!repository.updateStatus(claim, from)) {
            throw new BadRequestException("The claim was changed by someone else; reload the sheet");
        }
        if (status == ClaimStatus.OPEN) {
            // Numbered only once the claim is surely moving, so a refused open uses up no number.
            claim.setNumber(number(claim, repository, settings, sheets, periods));
            repository.updateStatus(claim, status);
        }
        audited.set(new ClaimNumber(claim.getNumber()));
    }

    /**
     * The number an opened claim takes: while the sheet numbers by period and its today is on or after that setting's
     * date, the next within the claim's period (by its date) as {@code <period name>-<n>}, or {@code X-<n>} in no period;
     * otherwise the sheet's next.
     */
    private static String number(Claim claim, ClaimRepository repository, SettingRepository settings,
            SheetRepository sheets, PeriodRepository periods) {
        String sheetId = claim.getSheetId();
        java.time.LocalDate from = settings.periodNumberingFrom(sheetId);
        if (from == null || sheets.today(sheetId).isBefore(from)) {
            return String.valueOf(repository.nextNumber(sheetId));
        }
        Period period = periods.on(sheetId, claim.getDate());
        return period == null ? "X-" + repository.nextNumber(sheetId, "X")
                : period.name() + "-" + repository.nextNumber(sheetId, String.valueOf(period.id()));
    }

    /** The figure a statement bound the claim to, else the claim's amount times its person's grade's rate on the claim's date, rounded as the claim rounds by the sheet's policy date and at least the sheet's floor (a correction has none); null when there is no rate. */
    private static BigDecimal dueNow(Claim claim, PersonRepository people, RateRepository rates,
            SettingRepository settings, LateCharge late) {
        if (claim.isStated()) {
            return claim.getStatedDue();
        }
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        BigDecimal rate = person == null ? null : rates.find(claim.getSheetId(), person.gradeOn(claim.getDate()), claim.getDate());
        return claim.figureAt(rate, settings.floor(claim.getSheetId()), late.rounding(claim));
    }
}
