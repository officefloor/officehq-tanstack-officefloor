package net.officefloor.hq.app.person;

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
 * Retires each selected person, in list order (see PersonRepository#list), through the very steps of the row's retire:
 * RetirePerson (from the date, blank: the sheet's today) and RecordPersonRetired (PERSON_RETIRED and the figures it
 * moved). One not on the sheet is skipped.
 */
public class RetirePeople {
    public void retire(@PathVariable("sheetId") String sheetId, @Val BulkRetireValues request, PersonRepository people,
            SheetRepository sheets, Rollup rollup, Audit audit, Out<BulkRetireResult> result) {
        String sheet = sheetId.trim();
        Set<Long> selected = new HashSet<>(request.personIds());
        RetirePerson apply = new RetirePerson();
        RecordPersonRetired record = new RecordPersonRetired();
        int retired = 0;
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
            apply.retire(person, request.retirement(), sheets, people, (p) -> changed[0] = p);
            record.record(changed[0], request.retirement(), before, rollup, audit);
            retired++;
        }
        result.set(new BulkRetireResult(retired));
    }
}
