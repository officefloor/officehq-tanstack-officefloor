package net.officefloor.hq.app.quickadd;

import net.officefloor.hq.app.person.AuditPersonAdded;
import net.officefloor.hq.app.person.DuplicatePersonNameException;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.PersonRequest;
import net.officefloor.hq.app.rollup.Rollups;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** The person on the sheet with the name (compared as the person form compares names); when there is none,
 *  add them with the sheet's default grade (A when blank; refused when another sheet of the sheet's tree has the name as a name or alias), recording the add and how the figures moved just as the person form does. */
public class FindOrAddQuickAddPerson {
    public void find(@PathVariable("sheetId") String sheetId, @Val PersonRequest request, PersonRepository repository,
            SettingRepository settings, Rollups rollups, Audit audit, Out<Person> person) throws DuplicatePersonNameException {
        String id = sheetId.trim();
        Person found = repository.findByName(id, request.name());
        if (found == null) {
            DuplicatePersonNameException.refuseInTree(id, request.name(), repository);
            var before = rollups.take(id);
            PersonRequest add = new PersonRequest(request.user(), request.name(), settings.gradeForNew(id),
                    request.version(), request.gradeFrom(), request.terms(), request.contact(), request.tags(),
                    request.termsFrom());
            found = repository.insert(id, add.name(), add.grade());
            new AuditPersonAdded().audit(add, found, audit);
            rollups.record(id, request.user(), before);
        }
        person.set(found);
    }
}
