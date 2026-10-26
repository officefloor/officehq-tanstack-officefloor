package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.access.RoleRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupSnapshot;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Retires each selected person from the date (the sheet's today when blank) in list order — the people in their list
 * positions — as the row's retire does: a PERSON_RETIRED line and the rollup lines it moved. A person retired already
 * is retired from the new date instead. A selected id not on this sheet is skipped. A viewer retires no one.
 */
public class RetirePeople {
    public void retire(@Val SheetResponse sheet, @Val BulkRetire bulk, @Val ChangedBy by, PersonRepository people,
            SheetRepository sheets, RoleRepository roles, Rollup rollup, Audit audit, Out<BulkRetireResponse> result) {
        if (roles.isViewer(sheet.id(), by.name())) {
            result.set(new BulkRetireResponse(0));
            return;
        }
        LocalDate from = bulk.date() != null ? bulk.date() : sheets.today(sheet.id());
        RecordPersonRetired record = new RecordPersonRetired();
        int retired = 0;
        for (Person person : people.list(sheet.id())) {
            if (!bulk.ids().contains(person.getId())) {
                continue;
            }
            RollupSnapshot before = rollup.snapshot(sheet.id());
            people.retireFrom(person.getId(), from);
            person.setRetiredFrom(from);
            record.record(person, by, audit);
            rollup.record(sheet.id(), before, by);
            retired++;
        }
        result.set(new BulkRetireResponse(retired));
    }
}
