package net.officefloor.hq.app.rollup;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/**
 * Records, before the sheet's today changes, a SNAPSHOT line per person in list order with their
 * due-total as it stood on the day being left, and keeps each on the person as a snapshot line.
 */
public class AuditSnapshot {
    public void audit(@Val Sheet sheet, @Val Rollup before, HttpServletRequest request, Audit audit,
            PersonRepository people) {
        String head = " sheet=" + sheet.id() + " by=" + Audit.user(request);
        before.people().forEach((id, figures) -> {
            people.addSnapshot(sheet.id(), id, sheet.today(), figures.dueTotal());
            audit.record("SNAPSHOT" + head + " name=" + figures.name() + " due-total=" + figures.dueTotal());
        });
    }
}
