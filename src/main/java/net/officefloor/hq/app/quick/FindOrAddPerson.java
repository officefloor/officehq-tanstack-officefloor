package net.officefloor.hq.app.quick;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRequest;
import net.officefloor.hq.app.rollup.Figures;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupLines;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class FindOrAddPerson {
    // The person on the sheet with the name (ignoring case), or a new one of grade A, recorded as an add of
    // a person would be. Publishes the figures after this step, for the claim's own rollup.
    public void find(@PathVariable("sheetId") String sheetId, @Val QuickName name, @Val Figures before,
            People people, Rollup rollup, ServerHttpConnection connection, Audit audit, Out<Person> personOut,
            Out<QuickFigures> figuresOut) {
        String sheet = sheetId.trim();
        Person person = people.findByName(sheet, name.name());
        Figures now = before;
        if (person == null) {
            person = people.add(sheet, new PersonRequest(name.name(), "A", null));
            String user = Audit.user(connection);
            audit.record(Audit.line("PERSON_ADDED", sheet, user, "name=" + person.name(), "grade=" + person.grade()));
            now = RollupLines.record(sheet, user, before, rollup, audit);
        }
        personOut.set(person);
        figuresOut.set(new QuickFigures(now));
    }
}
