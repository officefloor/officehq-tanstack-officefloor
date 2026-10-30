package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimNumbering;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * A steward clones the sheet to another id never used (see {@link SheetCloner} for what is copied). The claims are
 * copied person by person in list order, each person's in the order they are listed, with their status, frozen
 * figures, dates, notes and logs; a statement's binding is not. Each claim that had a number takes the clone's next
 * (by the clone's own numbering) in the order the claims were opened: by the sheet's today when each was opened, then
 * by the order they were added to the clone. Anyone but a steward, or another sheet already used, is refused.
 */
public class CloneSheet {
    public void cloneSheet(@PathVariable("sheetId") String sheetId, @RequestBody CloneRequest request, Sheets sheets,
            Settings settings, SheetCloner cloner, Claims claims, ClaimNumbering numbering, Rollup rollup, Audit audit,
            ObjectResponse<CloneResponse> response) throws ViewerRefusedException, InvalidCloneException {
        String user = request.user() == null ? "" : request.user().trim();
        String to = request.toSheet() == null ? "" : request.toSheet().trim();
        sheets.today(sheetId);
        if (!Settings.STEWARD.equals(settings.role(sheetId, user))) {
            throw new ViewerRefusedException("Only a steward can clone the sheet");
        }
        if (to.isEmpty()) {
            throw new InvalidCloneException("Name the sheet to clone to");
        }
        if (to.equals(sheetId) || sheets.exists(to)) {
            throw new InvalidCloneException("The sheet " + to + " is already used");
        }

        Map<Long, Long> people = cloner.copy(sheetId, to);
        // Every person and the totals are new on the clone.
        RollupFigures before = new RollupFigures(new LinkedHashMap<>(), "", "", "");

        List<Claim> copied = new ArrayList<>();
        Map<Long, Long> newIds = new HashMap<>();
        for (Map.Entry<Long, Long> person : people.entrySet()) {
            List<Claim> theirs = new ArrayList<>(claims.ofPerson(sheetId, person.getKey()));
            Collections.reverse(theirs); // oldest listed first, so they keep their order
            for (Claim claim : theirs) {
                newIds.put(claim.getId(), claims.cloneIn(claim, to, person.getValue()));
                copied.add(claim);
            }
        }
        List<Claim> numbered = new ArrayList<>(copied.stream().filter(c -> c.getNumber() != null).toList());
        Map<Long, LocalDate> openedOn = new HashMap<>();
        for (Claim claim : numbered) {
            openedOn.put(claim.getId(), claims.openedOn(claim));
        }
        numbered.sort(Comparator.comparing((Claim c) -> openedOn.get(c.getId()), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparingLong(c -> newIds.get(c.getId())));
        Map<String, String> renumbered = new HashMap<>();
        Map<Long, String> numbers = new HashMap<>();
        for (Claim claim : numbered) {
            String number = numbering.next(to, claim.getDate());
            renumbered.put(claim.getNumber(), number);
            numbers.put(claim.getId(), number);
        }
        for (Claim claim : copied) {
            claims.finishCloned(to, newIds.get(claim.getId()), numbers.get(claim.getId()),
                    claim.getCorrects() == null ? null : renumbered.get(claim.getCorrects()),
                    claim.getSplitFrom() == null ? null : renumbered.get(claim.getSplitFrom()));
        }

        audit.recordOnCommit("SHEET_CLONE_MADE sheet=" + sheetId + " by=" + user + " to=" + to);
        audit.recordOnCommit("SHEET_CLONED sheet=" + to + " by=" + user + " from=" + sheetId);
        rollup.recordChanges(to, user, before);
        response.send(new CloneResponse(sheetId, to));
    }
}
