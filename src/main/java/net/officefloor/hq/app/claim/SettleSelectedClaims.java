package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.watch.WatchNotices;
import net.officefloor.hq.app.watch.WatchRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Settles the selected claims in list order (people by their position in the list, each person's
 * claims newest first) by exactly the rules of settling one claim, as the signed-in user. A claim
 * sent to PENDING counts as settled. It is all or nothing: if any selected claim cannot be settled (its
 * status does not allow it, dated inside a closed period, opened or last edited by this user, not on
 * the sheet), none is settled, every one counts as refused and nothing is written. Otherwise the
 * claims form a batch, numbered on the sheet, and after their own lines a BATCH_SETTLED line gives
 * the batch, its count and the frozen figures of those SETTLED added up.
 * Each claim settled is logged and audited as a single settle is: its status line, then the
 * watchers it notifies (once settled), then the figures it moved. A viewer, or anyone on an archived
 * sheet, settles none: every selected claim is refused.
 */
public class SettleSelectedClaims {
    public void settle(@Val Sheet sheet, @Val BulkSettleRequest request, ClaimRepository claims,
            PersonRepository people, PeriodRepository periods, SheetRepository sheets, RateRepository rates,
            RoleRepository roles, SettingsRepository settings, ClaimLogRepository log, WatchRepository watches, HttpServletRequest http,
            Audit audit, Out<BulkSettleResponse> result) {
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
        int settled = 0;
        if (sheet.archived() || Role.VIEWER.equals(roles.roleOf(sheet.id(), user))) {
            result.set(new BulkSettleResponse(0, requested)); // a viewer, or an archived sheet, changes nothing
            return;
        }
        // All or nothing: every selected claim is settled in memory first; any refusal refuses them all.
        List<Claim> toSettle = new ArrayList<>();
        for (Person person : people.findAll(sheet.id())) {
            for (Claim claim : claims.findByPerson(sheet.id(), person.getId())) {
                if (!selected.contains(claim.getId())) continue;
                if (periods.closedOn(sheet.id(), claim.getDueBy()) != null) {
                    result.set(new BulkSettleResponse(0, requested));
                    return;
                }
                try {
                    SettleClaim.settle(sheet, claim, user, people, rates, roles, settings);
                } catch (InvalidClaimStatusException | SelfSettleException e) {
                    result.set(new BulkSettleResponse(0, requested));
                    return;
                }
                toSettle.add(claim);
            }
        }
        if (toSettle.isEmpty() || toSettle.size() != requested) {
            result.set(new BulkSettleResponse(0, requested)); // a selected id not on the sheet is refused
            return;
        }
        int batch = claims.nextBatch(sheet.id());
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        Rollup before = rollup(sheet, sheets, people, claims, rates, settings);
        for (Claim claim : toSettle) {
            claim.setSettleBatch(batch);
            claims.save(claim);
            log.record(claim, user, sheet.today(), LogClaimStatus.due(sheet, claim, people, rates, settings));
            settled++;
            if (claim.getStatus() == ClaimStatus.SETTLED && claim.getSettledDue() != null) {
                total = total.add(claim.getSettledDue());
            }
            audit.record(AuditClaimStatus.line(sheet, claim, user, people));
            WatchNotices.lines(sheet, claim, user, people, watches).forEach(audit::record);
            Rollup after = rollup(sheet, sheets, people, claims, rates, settings);
            after.changesSince(before, sheet.id(), user).forEach(audit::record);
            before = after;
        }
        audit.record("BATCH_SETTLED sheet=" + sheet.id() + " by=" + user + " batch=" + batch + " count=" + settled
                + " total=" + total.setScale(2, RoundingMode.HALF_UP).toPlainString());
        result.set(new BulkSettleResponse(settled, requested - settled));
    }

    private static Rollup rollup(Sheet sheet, SheetRepository sheets, PersonRepository people, ClaimRepository claims,
            RateRepository rates, SettingsRepository settings) {
        return Rollup.of(sheet.id(), sheets.today(sheet.id()), people, claims, rates, settings.find(sheet.id()));
    }
}
