package net.officefloor.hq.app.quick;

import net.officefloor.hq.app.person.DuplicatePersonNameException;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * The person on the sheet with the typed name (ignoring case), or a new one with the sheet's default grade (A when it is retired), not yet saved:
 * refused when the name is a name or alias on another sheet of the sheet's tree.
 */
public class FindQuickAddPerson {
    public void find(@Val QuickAdd quick, @Val Sheet sheet, PersonRepository repository, SheetRepository sheets,
            net.officefloor.hq.app.setting.SettingsRepository settings, net.officefloor.hq.app.grade.GradeRepository grades,
            Out<Person> personOut, Out<QuickAddPerson> createdOut) throws DuplicatePersonNameException {
        Person person = repository.findByName(sheet.id(), quick.name());
        if (person == null && repository.nameTakenIn(sheets.tree(sheet.id()), quick.name(), null)) {
            throw new DuplicatePersonNameException(quick.name());
        }
        createdOut.set(new QuickAddPerson(person == null));
        personOut.set(person != null ? person : new Person(null, sheet.id(), quick.name(),
                grades.newPersonGrade(settings.find(sheet.id()))));
    }
}
