package net.officefloor.hq.app.rollover;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.period.InvalidPeriodException;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Refuse an undo unless the sheet's last rollover is not yet undone, was made on the sheet's today by the same user
 *  (still a steward), and none of the periods it generated has a claim (of any status) dated or due in it; otherwise
 *  publish the rollover to undo. */
public class CheckRolloverUndo {
    public void check(@PathVariable("sheetId") String sheetId, @Val RolloverUndoInput input,
            RolloverRepository rollovers, PeriodRepository periods, ClaimRepository claims, RoleRepository roles,
            SheetRepository sheets, Out<RolloverRepository.Rollover> undo) throws InvalidPeriodException {
        String id = sheetId.trim();
        periods.lockSheet(id);
        if (!Role.STEWARD.equals(roles.of(id, input.user()))) {
            throw new InvalidPeriodException("Only a steward may undo a rollover on this sheet.");
        }
        RolloverRepository.Rollover last = rollovers.last(id);
        if (last == null || last.undone()) {
            throw new InvalidPeriodException("There is no rollover to undo on this sheet.");
        }
        if (!last.madeBy().equals(input.user())) {
            throw new InvalidPeriodException("Only " + last.madeBy() + ", who made the last rollover, may undo it.");
        }
        if (!Objects.equals(last.madeOn(), sheets.today(id))) {
            throw new InvalidPeriodException("The last rollover can only be undone on the day it was made.");
        }
        Set<String> names = Set.copyOf(last.periodNames());
        var generated = periods.list(id).stream().filter(p -> names.contains(p.name())).toList();
        Set<String> taken = claims.list(id).stream()
                .flatMap(c -> generated.stream().filter(p -> p.contains(c.date())
                        || (c.dueBy() != null && p.contains(c.dueBy()))))
                .map(Period::name).collect(Collectors.toCollection(java.util.TreeSet::new));
        if (!taken.isEmpty()) {
            throw new InvalidPeriodException("A claim now belongs to " + String.join(", ", taken)
                    + ", which the rollover generated.");
        }
        undo.set(last);
    }
}
