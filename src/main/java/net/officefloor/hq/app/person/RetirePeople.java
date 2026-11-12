package net.officefloor.hq.app.person;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Retires each selected person from the date, in list order (by their positions), exactly as the row's
 *  retire would, each recorded as PERSON_RETIRED and then how it moved the sheet's figures. A selected
 *  person not on the sheet refuses the whole change; a viewer retires no one (0, nothing stored). */
public class RetirePeople {
    public void retire(@PathVariable("sheetId") String sheetId, @Val BulkRetireInput input, PersonStore store,
            RoleStore roles, RollupService rollups, Audit audit, Out<BulkRetireResult> result)
            throws InvalidSheetException {
        String sheet = sheetId.trim();
        List<Person> selected = new ArrayList<>();
        for (Long id : input.personIds()) {
            Person person = store.find(sheet, id);
            if (person == null) {
                throw new InvalidSheetException("No such person: " + id);
            }
            selected.add(person);
        }
        if (roles.find(sheet, input.user() == null ? "" : input.user().trim()) == Role.VIEWER) {
            result.set(new BulkRetireResult(0));
            return;
        }
        List<Long> order = store.list(sheet).stream().map(Person::getId).toList();
        selected.sort(Comparator.comparingInt(p -> order.indexOf(p.getId())));
        for (Person person : selected) {
            Rollup before = rollups.snapshot(sheet);
            store.retire(person, input.from());
            audit.record("PERSON_RETIRED sheet=" + sheet + " by=" + input.user() + " name=" + person.getName()
                    + " from=" + person.getRetiredFrom());
            rollups.record(sheet, input.user(), before);
        }
        result.set(new BulkRetireResult(selected.size()));
    }
}
