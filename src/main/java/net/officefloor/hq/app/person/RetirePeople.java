package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.Snapshot;
import net.officefloor.hq.app.settings.Role;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Retires each selected person from the date, in list order, exactly as the row's retire would:
 *  each audited PERSON_RETIRED, then the figures it moved. One not on the sheet is skipped. A
 *  viewer, or anyone on an archived sheet, retires no one. */
public class RetirePeople {
    public void retire(@PathVariable("sheetId") String sheetId, @Val BulkRetire bulk, People people, Sheets sheets,
            Rollup rollup, Settings settings, Audit audit, Out<BulkRetireResult> result) {
        Retirement retirement = bulk.retirement();
        if (sheets.archived(sheetId) || settings.role(sheetId, retirement.user()) == Role.VIEWER) {
            result.set(new BulkRetireResult(0));
            return;
        }
        RetirePerson retire = new RetirePerson();
        RecordPersonRetired record = new RecordPersonRetired();
        int retired = 0;
        for (Person listed : people.onSheet(sheetId)) {
            if (!bulk.personIds().contains(listed.getId())) continue;
            Person person = people.find(sheetId, listed.getId());
            if (person == null) continue;
            Snapshot before = rollup.snapshot(sheetId);
            retire.retire(retirement, person, sheets, people);
            record.record(retirement, person, audit);
            rollup.record(sheetId, before, retirement.user());
            retired++;
        }
        result.set(new BulkRetireResult(retired));
    }
}
