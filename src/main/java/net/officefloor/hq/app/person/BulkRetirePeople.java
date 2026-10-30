package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Retires the selected people in list order (the order they are listed), each exactly as the row's
 * retire would: from the date (the sheet's today when blank), audited as PERSON_RETIRED with the rollup
 * lines it moved, and counted. A selected id that is not a person on the sheet is skipped.
 */
public class BulkRetirePeople {
    public void retire(@PathVariable("sheetId") String sheetId, @Val ValidBulkRetire request,
            People people, Sheets sheets, Rollup rollup, Audit audit, ObjectResponse<BulkRetireResponse> response) {
        Set<String> selected = new HashSet<>(request.personIds());
        int retired = 0;
        for (Person person : people.list(sheetId)) {
            if (!selected.contains(String.valueOf(person.getId()))) {
                continue;
            }
            RollupFigures before = rollup.figures(sheetId);
            LocalDate from = request.date() != null ? request.date() : sheets.today(sheetId);
            person.setRetiredFrom(from);
            people.saveRetired(person);
            audit.recordOnCommit("PERSON_RETIRED sheet=" + sheetId + " by=" + request.user()
                    + " name=" + person.getName() + " from=" + from);
            rollup.recordChanges(sheetId, request.user(), before);
            retired++;
        }
        response.send(new BulkRetireResponse(retired));
    }
}
