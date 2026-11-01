package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.Snapshot;
import net.officefloor.hq.app.settings.Role;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Holds each selected person, in list order, exactly as the row's hold would: each audited
 *  PERSON_HELD, then the figures it moved. One not on the sheet, or one the row would refuse, is
 *  skipped. A viewer, or anyone on an archived sheet, holds no one. */
public class HoldPeople {
    public void hold(@PathVariable("sheetId") String sheetId, @Val BulkHold bulk, People people, Sheets sheets,
            Rollup rollup, Settings settings, Audit audit, Out<BulkHoldResult> result) {
        HoldChange change = bulk.change();
        if (sheets.archived(sheetId) || settings.role(sheetId, change.user()) == Role.VIEWER) {
            result.set(new BulkHoldResult(0));
            return;
        }
        RecordPersonHeld record = new RecordPersonHeld();
        Actor actor = new Actor(change.user());
        int held = 0;
        for (Person listed : people.onSheet(sheetId)) {
            if (!bulk.personIds().contains(listed.getId())) continue;
            Snapshot before = rollup.snapshot(sheetId);
            Person person = people.lock(sheetId, listed.getId());
            if (person == null) continue;
            LocalDate from = change.from() != null ? change.from() : sheets.today(sheetId);
            if (change.to() != null && change.to().isBefore(from)) continue;
            Hold added = new Hold(from, change.to());
            people.addHold(person, added);
            record.record(person, added, actor, audit);
            rollup.record(sheetId, before, change.user());
            held++;
        }
        result.set(new BulkHoldResult(held));
    }
}
