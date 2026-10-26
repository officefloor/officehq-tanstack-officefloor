package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Adds the person; their own terms from the request's terms from-date, else from the beginning of time. */
public class BuildPerson {
    public void build(@Val SheetResponse sheet, @Val PersonRequest request, PersonRepository repository,
            SheetRepository sheets, Out<Person> person) {
        LocalDate from = request.termsFromDate();
        Person added = repository.insert(sheet.id(), request.name(), request.grade(), from == null ? request.termsDays() : null,
                request.contactOrNull(), request.tagsOrNull());
        if (from != null) {
            repository.giveTerms(added, request.termsDays(), from);
        }
        added.setToday(sheets.today(sheet.id()));
        person.set(added);
    }
}
