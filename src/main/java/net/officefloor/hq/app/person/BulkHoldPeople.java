package net.officefloor.hq.app.person;

import java.util.HashSet;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.pool.PoolRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetArchivedException;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Adds the hold on the selected people in list order (by their places in the list), each as the
 *  row's hold would (see {@link HoldPerson}: from the from-date, blank the sheet's today, to the
 *  to-date, blank none) and recorded as it would record it (its PERSON_HELD line, then the rollup
 *  lines it moved). A selected id not on the sheet is skipped. A viewer may hold no one: none are
 *  held. Refused on an archived sheet, or when the to-date is before a defaulted from-date. */
public class BulkHoldPeople {
    public void hold(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by, @Val BulkHoldRequest request,
            PersonRepository people, ClaimRepository claims, RateRepository rates,
            SheetRepository sheets, SettingRepository settings, PoolRepository pools, RoleRepository roles, Audit audit,
            Out<BulkHoldResult> result) throws SheetArchivedException, InvalidPersonException {
        String id = sheetId.trim();
        String user = by == null ? "" : by;
        if (sheets.isArchived(id)) {
            throw new SheetArchivedException("The sheet is archived");
        }
        if (Role.VIEWER.equals(roles.roleOf(id, user))) {
            result.set(new BulkHoldResult(0));
            return;
        }
        String from = request.from() != null ? request.from() : sheets.today(id).toString();
        if (request.to() != null && request.to().compareTo(from) < 0) {
            throw new InvalidPersonException("The hold ends before it starts: " + from + " to " + request.to());
        }
        Set<Long> selected = new HashSet<>(request.personIds());
        int held = 0;
        Rollup before = Rollup.of(id, people, claims, rates, sheets, settings, pools);
        for (Person listed : people.list(id)) {
            if (!selected.contains(listed.getId())) continue;
            Person person = people.find(id, listed.getId());
            people.addHold(person, new Hold(from, request.to()), user);
            held++;
            audit.record("PERSON_HELD sheet=" + id + " by=" + user + " name=" + person.getName()
                    + " from=" + from);
            Rollup after = Rollup.of(id, people, claims, rates, sheets, settings, pools);
            for (String line : after.changesSince(before, id, user)) {
                audit.record(line);
            }
            before = after;
        }
        result.set(new BulkHoldResult(held));
    }
}
