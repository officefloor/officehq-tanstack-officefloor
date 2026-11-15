package net.officefloor.hq.app.person;

import java.util.HashSet;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.ClaimRepository;
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

/** Retires the selected people in list order (by their places in the list), each as the row's
 *  retire would (see {@link RetirePerson}: from the date, blank the sheet's today) and recorded as
 *  it would record it (its PERSON_RETIRED line, then the rollup lines it moved). A selected id not
 *  on the sheet is skipped. A viewer may retire no one: none are retired. Refused on an archived
 *  sheet. */
public class BulkRetirePeople {
    public void retire(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by, @Val BulkRetireRequest request,
            PersonRepository people, ClaimRepository claims, RateRepository rates,
            SheetRepository sheets, SettingRepository settings, RoleRepository roles, Audit audit,
            Out<BulkRetireResult> result) throws SheetArchivedException {
        String id = sheetId.trim();
        String user = by == null ? "" : by;
        if (sheets.isArchived(id)) {
            throw new SheetArchivedException("The sheet is archived");
        }
        if (Role.VIEWER.equals(roles.roleOf(id, user))) {
            result.set(new BulkRetireResult(0));
            return;
        }
        Set<Long> selected = new HashSet<>(request.personIds());
        String from = request.from() != null ? request.from() : sheets.today(id).toString();
        int retired = 0;
        Rollup before = Rollup.of(id, people, claims, rates, sheets, settings);
        for (Person listed : people.list(id)) {
            if (!selected.contains(listed.getId())) continue;
            Person person = people.find(id, listed.getId());
            person.setRetiredFrom(from);
            people.updateRetired(person);
            retired++;
            audit.record("PERSON_RETIRED sheet=" + id + " by=" + user + " name=" + person.getName()
                    + " from=" + person.getRetiredFrom());
            Rollup after = Rollup.of(id, people, claims, rates, sheets, settings);
            for (String line : after.changesSince(before, id, user)) {
                audit.record(line);
            }
            before = after;
        }
        result.set(new BulkRetireResult(retired));
    }
}
