package net.officefloor.hq.app.rollover;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Undoes the sheet's last rollover: removes the periods it generated and takes back the opening balances it set, then
 * records ROLLOVER_UNDONE fy=FY&lt;old year&gt;. Refuses when there is no rollover left to undo, when the sheet's today
 * is not the one it was made on, when anyone but the steward who made it asks, and when a claim's due-by now falls in
 * one of the periods it generated.
 */
public class ApplyRolloverUndo {
    public void apply(@Val Sheet sheet, LastRolloverRepository lastRollovers, PeriodRepository periods,
            ClaimRepository claims, RoleRepository roles,
            @RequestHeader(name = "X-User", required = false) String user, Audit audit)
            throws InvalidRolloverException {
        String by = AuditUser.decode(user);
        if (!Role.STEWARD.equals(roles.roleOf(sheet.getId(), by))) {
            throw new InvalidRolloverException("Only a steward can undo a rollover.");
        }
        LastRollover last = lastRollovers.findById(sheet.getId()).orElse(null);
        if (last == null) {
            throw new InvalidRolloverException("There is no rollover to undo.");
        }
        if (!last.getMadeBy().equals(by)) {
            throw new InvalidRolloverException("Only the steward who made the rollover can undo it.");
        }
        if (!last.getMadeOn().equals(sheet.getToday())) {
            throw new InvalidRolloverException("A rollover can only be undone on the day it was made.");
        }
        Set<Long> generatedIds = new HashSet<>(last.getPeriodIds());
        List<Period> generated = periods.findBySheetIdOrderByFromDateAscIdAsc(sheet.getId()).stream()
                .filter(p -> generatedIds.contains(p.getId())).toList();
        boolean claimed = claims.findBySheetIdOrderByIdDesc(sheet.getId()).stream()
                .anyMatch(c -> c.getDueBy() != null && generated.stream().anyMatch(p -> p.contains(c.getDueBy())));
        if (claimed) {
            throw new InvalidRolloverException("A claim now belongs to a period the rollover generated.");
        }

        for (LastRollover.Opening opening : last.getOpenings()) {
            if (generatedIds.contains(opening.getPeriodId())) {
                continue;
            }
            periods.findByIdAndSheetId(opening.getPeriodId(), sheet.getId()).ifPresent(period -> {
                period.addOpening(opening.getPersonId(), opening.getAmount().negate());
                if (period.getPersonOpening().get(opening.getPersonId()).signum() == 0) {
                    period.getPersonOpening().remove(opening.getPersonId());
                }
                periods.save(period);
            });
        }
        periods.deleteAll(generated);
        lastRollovers.delete(last);
        periods.flush();
        audit.record("ROLLOVER_UNDONE sheet=" + sheet.getId() + " by=" + by + " fy=FY" + last.getFiscalYear());
    }
}
