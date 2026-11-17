package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Holds each selected person, in list order (see PersonRepository#list), through the very steps of the row's hold:
 * HoldPerson (from the from-date, blank: the sheet's today, until the to-date, if any) and RecordPersonHold
 * (PERSON_HELD and the figures it moved). A to-date before the from-date is refused before anyone is held. One not on
 * the sheet is skipped.
 */
public class HoldPeople {
    public void hold(@PathVariable("sheetId") String sheetId, @Val BulkHoldValues request, PersonRepository people,
            SheetRepository sheets, Rollup rollup, Audit audit, Out<BulkHoldResult> result) throws InvalidPersonException {
        String sheet = sheetId.trim();
        Hold hold = request.hold();
        LocalDate from = hold.from() != null ? hold.from() : sheets.today(sheet);
        if (hold.to() != null && hold.to().isBefore(from)) {
            throw new InvalidPersonException("The hold's to-date must not be before its from-date");
        }
        Set<Long> selected = new HashSet<>(request.personIds());
        HoldPerson apply = new HoldPerson();
        RecordPersonHold record = new RecordPersonHold();
        int held = 0;
        for (Person listed : people.list(sheet)) {
            if (!selected.contains(listed.id())) {
                continue;
            }
            Person person = people.find(sheet, listed.id());
            if (person == null) {
                continue;
            }
            RollupBefore before = rollup.take(sheet);
            Person[] changed = new Person[1];
            PersonHold[] added = new PersonHold[1];
            apply.hold(person, hold, sheets, people, (p) -> changed[0] = p, (h) -> added[0] = h);
            record.record(changed[0], hold, added[0], before, rollup, audit);
            held++;
        }
        result.set(new BulkHoldResult(held));
    }
}
