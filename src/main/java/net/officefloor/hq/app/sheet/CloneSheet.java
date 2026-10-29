package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class CloneSheet {
    // Only a steward (a delegation included) clones the sheet, and only to a sheet id never used. Each numbered claim
    // takes the next number of the clone, in the order the originals were opened (the sheet's today when each was first
    // opened, then the order they were added to the clone).
    public void clone(@PathVariable("sheetId") String sheetId, @RequestBody(required = false) CloneRequest body,
            Settings settings, Sheets sheets, SheetClones clones, Claims claims, Periods periods,
            ServerHttpConnection connection, Out<ClonedSheet> clonedOut) throws SheetCloneRefusedException {
        String from = sheetId.trim();
        if (!Settings.STEWARD.equals(settings.role(from, Audit.user(connection).trim()))) {
            throw new SheetCloneRefusedException("Only a steward can clone the sheet.");
        }
        String to = body == null || body.toSheetId() == null ? "" : body.toSheetId().trim();
        if (to.isEmpty()) {
            throw new SheetCloneRefusedException("Give the id of the sheet to clone to.");
        }
        if (to.equals(from) || sheets.exists(to) || clones.used(to)) {
            throw new SheetCloneRefusedException("The sheet " + to + " is already used: a clone goes only to a new sheet.");
        }

        Map<Long, Long> copied = clones.copy(from, to, sheets.find(from).today());

        // The numbered claims in the order they were opened, each given the next number of the clone.
        Map<Long, LocalDate> opened = claims.openedOn(from);
        Map<Long, Claim> originals = new HashMap<>();
        claims.list(from).forEach(c -> originals.put(c.id(), c));
        List<Long> numbered = new ArrayList<>(copied.keySet().stream()
                .filter(id -> originals.get(id).number() != null).toList());
        numbered.sort(Comparator.<Long, LocalDate>comparing(id -> opened.getOrDefault(id, originals.get(id).date()))
                .thenComparingLong(copied::get));
        LocalDate numberingFrom = settings.periodNumberingFrom(to);
        boolean byPeriod = numberingFrom != null && !sheets.today(to).isBefore(numberingFrom);
        Map<String, String> renumbered = new HashMap<>();
        Map<Long, String> numbers = new HashMap<>();
        for (Long id : numbered) {
            Claim claim = originals.get(id);
            String scope = null;
            if (byPeriod) {
                Period period = periods.on(to, claim.dueBy());
                scope = period == null ? "" : period.name();
            }
            String number = claims.number(to, scope);
            numbers.put(copied.get(id), number);
            renumbered.put(claim.number(), number);
        }
        clones.number(copied, originals, numbers, renumbered);
        clonedOut.set(new ClonedSheet(from, to));
    }
}
