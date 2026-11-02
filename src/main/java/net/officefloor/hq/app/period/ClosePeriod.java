package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

/** Close a period: fix its due-total as shown now (the figures of the counting claims dated in it,
 *  skipping those with none), so later changes to rates do not alter it. */
public class ClosePeriod {
    public void close(@PathVariable("sheetId") String sheetId, @PathVariable("periodName") String periodName,
            ServerHttpConnection connection, PeriodRepository repository, ClaimRepository claims, Audit audit,
            Out<Period> period) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        if (!repository.lockSheet(id)) {
            throw new BadRequestException("no such sheet");
        }
        Period found = repository.find(id, periodName == null ? "" : periodName.trim());
        if (found == null) {
            throw new BadRequestException("no such period");
        }
        if (found.closed()) {
            throw new BadRequestException("the period " + found.name() + " is already closed");
        }
        LocalDate from = LocalDate.parse(found.from());
        LocalDate to = LocalDate.parse(found.to());
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (Claim c : claims.list(id)) {
            LocalDate on = LocalDate.parse(c.date());
            if (!c.counts() || on.isBefore(from) || on.isAfter(to)) continue;
            BigDecimal figure = claims.figure(id, c);
            if (figure != null) total = total.add(figure);
        }
        String shown = total.setScale(2).toPlainString();
        repository.close(id, found.id(), total);
        audit.record("PERIOD_CLOSED sheet=" + id + " by=" + by + " period=" + found.name() + " total=" + shown);
        period.set(new Period(found.id(), found.name(), found.from(), found.to(), true, shown));
    }
}
