package net.officefloor.hq.app.claim;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.PeriodStore;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Opens each selected claim that can be opened, in list order: its people in the order they were
 *  added, a person's claims newest first. Each is opened as a single open would be (its next
 *  number, opened-by the user) and recorded as CLAIM_OPENED, then how it moved the sheet's figures.
 *  A claim that is not on the sheet, is not a DRAFT, or is dated in a closed period, and every claim when
 *  the sheet is archived or the sheet's permission matrix does not allow the user's role to open, is refused and
 *  left as it is. */
public class OpenClaims {
    public void open(@PathVariable("sheetId") String sheetId, @Val BulkOpenInput input, ClaimStore store,
            PersonStore people, PeriodStore periods, SettingStore settings, SheetStore sheets, RoleStore roles, ClaimLogStore log, RollupService rollups, Audit audit, Out<BulkOpenResult> result) {
        String sheet = sheetId.trim();
        Map<Long, Integer> personOrder = new HashMap<>();
        Map<Long, Person> persons = new HashMap<>();
        for (Person person : people.list(sheet)) {
            personOrder.put(person.getId(), personOrder.size());
            persons.put(person.getId(), person);
        }
        int refused = 0;
        List<Claim> selected = new ArrayList<>();
        for (Long id : input.claimIds()) {
            Claim claim = id == null ? null : store.find(sheet, id);
            if (claim == null) {
                refused++;
            } else {
                selected.add(claim);
            }
        }
        selected.sort(Comparator.<Claim>comparingInt(c -> personOrder.getOrDefault(c.getPersonId(), Integer.MAX_VALUE))
                .thenComparing(Comparator.comparingLong(Claim::getId).reversed()));
        boolean permitted = !sheets.archived(sheet) && settings.allows(sheet, "open", roles.find(sheet, input.user()));
        int opened = 0;
        for (Claim claim : selected) {
            if (!permitted || claim.getStatus() != ClaimStatus.DRAFT || closed(periods, claim)) {
                refused++;
                continue;
            }
            Rollup before = rollups.snapshot(sheet);
            if (!store.move(claim, claim.getStatus(), ClaimStatus.OPEN)) {
                refused++;
                continue;
            }
            claim.setStatus(ClaimStatus.OPEN);
            claim.setNumber(Numbering.next(sheet, claim.getDate(), store, settings, sheets, periods));
            claim.setOpenedBy(input.user());
            store.save(claim);
            log.record(claim, input.user());
            Person person = persons.get(claim.getPersonId());
            ClaimResponse shown = ClaimResponse.from(claim);
            audit.record(ClaimStatus.OPEN.event() + " sheet=" + sheet + " by=" + input.user() + " person="
                    + (person == null ? "" : person.getName()) + " amount=" + shown.amount() + " date=" + shown.date()
                    + " number=" + claim.getNumber());
            rollups.record(sheet, input.user(), before);
            opened++;
        }
        result.set(new BulkOpenResult(opened, refused));
    }

    private static boolean closed(PeriodStore periods, Claim claim) {
        try {
            CheckClaimPeriod.refuseClosed(periods, claim.getSheetId(), claim.getDate());
            return false;
        } catch (InvalidSheetException e) {
            return true;
        }
    }
}
