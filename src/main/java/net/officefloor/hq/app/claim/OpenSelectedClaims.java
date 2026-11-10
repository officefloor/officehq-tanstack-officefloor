package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Opens the selected claims in list order (people in the order they were added, each person's
 * claims newest first), numbering each as it is opened. A claim that cannot be opened (not a
 * draft, dated inside a closed period, not on the sheet) is refused and left as it is. Each claim
 * opened is audited as a single open is: its CLAIM_OPENED line, then the figures it moved.
 */
public class OpenSelectedClaims {
    public void open(@Val Sheet sheet, @Val BulkOpenRequest request, ClaimRepository claims,
            PersonRepository people, PeriodRepository periods, SheetRepository sheets, RateRepository rates,
            SettingsRepository settings, HttpServletRequest http, Audit audit, Out<BulkOpenResponse> result) {
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
        Rollup before = rollup(sheet, sheets, people, claims, rates, settings);
        for (Person person : people.findAll(sheet.id())) {
            List<Claim> theirs = claims.findByPerson(sheet.id(), person.getId());
            for (Claim claim : theirs) {
                if (!selected.contains(claim.getId())) continue;
                if (!opens(sheet, claim, periods, claims)) continue;
                claim.setOpenedBy(user);
                claim.setNumber(ClaimNumbers.next(sheet, claim, settings.find(sheet.id()), periods, claims));
                claims.save(claim);
                opened++;
                audit.record(ClaimAudit.line(claim.getStatus().auditEvent(), sheet, user, person, claim));
                Rollup after = rollup(sheet, sheets, people, claims, rates, settings);
                after.changesSince(before, sheet.id(), user).forEach(audit::record);
                before = after;
            }
        }
        result.set(new BulkOpenResponse(opened, requested - opened));
    }

    /** Moves the claim to OPEN when its status and period allow it and no one opened it first. */
    private static boolean opens(Sheet sheet, Claim claim, PeriodRepository periods, ClaimRepository claims) {
        if (periods.closedOn(sheet.id(), claim.getDate()) != null) return false;
        try {
            claim.moveTo(ClaimStatus.OPEN);
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
