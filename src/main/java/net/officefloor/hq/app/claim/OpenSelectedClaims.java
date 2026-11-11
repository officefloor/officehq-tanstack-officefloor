package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.PeriodCaps;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Opens the selected claims in list order (people by their position in the list, each person's
 * claims newest first), numbering each as it is opened. A claim that cannot be opened (not a
 * draft, dated inside a closed period, its person at its period's cap, not on the sheet, above the note threshold with an empty note, its person flagged and the user no steward) is refused and left as it is; every one is
 * refused when the permission matrix does not allow the user's role to open, or the sheet is
 * archived. A claim sent to REVIEW (above the review threshold) counts as opened. Each claim opened is logged and audited as a single open is: its CLAIM_OPENED line,
 * then the figures it moved.
 */
public class OpenSelectedClaims {
    public void open(@Val Sheet sheet, @Val BulkOpenRequest request, ClaimRepository claims,
            PersonRepository people, PeriodRepository periods, SheetRepository sheets, RateRepository rates,
            RoleRepository roles, SettingsRepository settings, ClaimLogRepository log, HttpServletRequest http, Audit audit,
            Out<BulkOpenResponse> result) {
        String user = Audit.user(http);
        Set<Long> selected = new HashSet<>();
        for (String id : request.claimIds()) {
            try {
                selected.add(Long.parseLong(id.trim()));
            } catch (NumberFormatException | NullPointerException e) {
                // not an id: no claim on the sheet, so refused below
            }
        }
        int requested = new HashSet<>(request.claimIds()).size();
        int opened = 0;
        if (sheet.archived() || !settings.find(sheet.id()).allows("open", roles.roleOf(sheet.id(), user))) {
            result.set(new BulkOpenResponse(0, requested));
            return;
        }
        String role = roles.roleOf(sheet.id(), user);
        Rollup before = rollup(sheet, sheets, people, claims, rates, settings);
        for (Person person : people.findAll(sheet.id())) {
            List<Claim> theirs = claims.findByPerson(sheet.id(), person.getId());
            for (Claim claim : theirs) {
                if (!selected.contains(claim.getId())) continue;
                if (person.flagRefuses(role, sheet.today())) continue;
                if (OpenClaim.needsNote(sheet, claim, people, rates, settings)) continue;
                if (!opens(sheet, claim, periods, claims, people, rates, settings)) continue;
                claim.setOpenedBy(user);
                if (claim.getStatus() == ClaimStatus.OPEN) {
                    claim.setNumber(ClaimNumbers.next(sheet, claim, settings.find(sheet.id()), periods, claims));
                }
                claims.save(claim);
                log.record(claim, user, sheet.today(), LogClaimStatus.due(sheet, claim, people, rates, settings));
                opened++;
                audit.record(ClaimAudit.line(claim.getStatus().auditEvent(), sheet, user, person, claim));
                Rollup after = rollup(sheet, sheets, people, claims, rates, settings);
                after.changesSince(before, sheet.id(), user).forEach(audit::record);
                before = after;
            }
        }
        result.set(new BulkOpenResponse(opened, requested - opened));
    }

    /**
     * Moves the claim to OPEN (or REVIEW, above the review threshold) when its status, period and period's cap
     * allow it and no one opened it first.
     */
    private static boolean opens(Sheet sheet, Claim claim, PeriodRepository periods, ClaimRepository claims,
            PersonRepository people, RateRepository rates, SettingsRepository settings) {
        if (periods.closedOn(sheet.id(), claim.getDueBy()) != null) return false;
        if (PeriodCaps.full(sheet, claim, periods, claims) != null) return false;
        try {
            claim.moveTo(OpenClaim.opensTo(sheet, claim, people, rates, settings));
        } catch (InvalidClaimStatusException e) {
            return false;
        }
        return claims.markOpened(claim);
    }

    private static Rollup rollup(Sheet sheet, SheetRepository sheets, PersonRepository people, ClaimRepository claims,
            RateRepository rates, SettingsRepository settings) {
        return Rollup.of(sheet.id(), sheets.today(sheet.id()), people, claims, rates, settings.find(sheet.id()));
    }
}
