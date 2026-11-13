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

/** Gives each selected person the hold, in list order (by their positions), exactly as the row's hold
 *  would, each recorded as PERSON_HELD and then how it moved the sheet's figures. A selected person
 *  not on the sheet refuses the whole change; a viewer holds no one (0, nothing stored). */
public class HoldPeople {
    public void hold(@PathVariable("sheetId") String sheetId, @Val BulkHoldInput input, PersonStore store,
            RoleStore roles, RollupService rollups, Audit audit, Out<BulkHoldResult> result)
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
        String user = input.user() == null ? "" : input.user().trim();
        if (roles.find(sheet, user) == Role.VIEWER) {
            result.set(new BulkHoldResult(0));
            return;
        }
        List<Long> order = store.list(sheet).stream().map(Person::getId).toList();
        selected.sort(Comparator.comparingInt(p -> order.indexOf(p.getId())));
        for (Person person : selected) {
            Rollup before = rollups.snapshot(sheet);
            store.hold(person, input.hold());
            audit.record("PERSON_HELD sheet=" + sheet + " by=" + input.user() + " name=" + person.getName()
                    + " from=" + input.hold().from());
            rollups.record(sheet, input.user(), before);
        }
        result.set(new BulkHoldResult(selected.size()));
    }
}
