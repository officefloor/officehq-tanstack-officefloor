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
 * Holds the selected people in list order (the order they are listed), each exactly as the row's hold
 * would: from the from-date (the sheet's today when blank) to the optional to-date, audited as PERSON_HELD
 * with the rollup lines it moved, and counted. A to-date before the from-date is refused for everyone and
 * writes nothing. A selected id that is not a person on the sheet is skipped.
 */
public class BulkHoldPeople {
    public void hold(@PathVariable("sheetId") String sheetId, @Val ValidBulkHold request,
            People people, Sheets sheets, Rollup rollup, Audit audit, ObjectResponse<BulkHoldResponse> response)
            throws InvalidPersonException {
        Set<String> selected = new HashSet<>(request.personIds());
        int held = 0;
        if (!selected.isEmpty()) {
            LocalDate from = request.from() != null ? request.from() : sheets.today(sheetId);
            if (request.to() != null && request.to().isBefore(from)) {
                throw new InvalidPersonException("The hold's to-date is before its from-date");
            }
            for (Person person : people.list(sheetId)) {
                if (!selected.contains(String.valueOf(person.getId()))) {
                    continue;
                }
                RollupFigures before = rollup.figures(sheetId);
                people.addHold(person, new Person.Hold(from, request.to()));
                audit.recordOnCommit("PERSON_HELD sheet=" + sheetId + " by=" + request.user()
                        + " name=" + person.getName() + " from=" + from);
                rollup.recordChanges(sheetId, request.user(), before);
                held++;
            }
        }
        response.send(new BulkHoldResponse(held));
    }
}
