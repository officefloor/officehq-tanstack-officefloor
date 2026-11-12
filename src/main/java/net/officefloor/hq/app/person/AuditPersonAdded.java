package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Val;

/** Records a stored person add: PERSON_ADDED sheet=<id> by=<user> name=<name> grade=<grade>, then
 *  terms=<days> when the form gave terms (those in force on the sheet's today when dated), then contact=<contact> when it gave a contact. */
public class AuditPersonAdded {
    public void audit(@Val PersonRequest request, @Val Person person, @Val Rollup before, Audit audit,
            RollupService rollups, SheetStore sheets) {
        Integer terms = request.termsFrom() == null ? person.getTerms()
                : person.termsOn(sheets.today(person.getSheetId()));
        audit.record("PERSON_ADDED sheet=" + person.getSheetId() + " by=" + request.user() + " name="
                + person.getName() + " grade=" + request.grade()
                + (terms == null ? "" : " terms=" + terms)
                + (person.getContact() == null ? "" : " contact=" + person.getContact()));
        rollups.record(person.getSheetId(), request.user(), before);
    }
}
