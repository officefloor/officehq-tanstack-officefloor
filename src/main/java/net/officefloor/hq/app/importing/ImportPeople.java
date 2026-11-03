package net.officefloor.hq.app.importing;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.person.NewPerson;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.PersonRequest;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

/** Add a person per imported line, in order, as the person form does. One change: every line is checked
 *  first, so a refused line adds no one and writes no audit record; otherwise each person is added and
 *  written as PERSON_ADDED. */
public class ImportPeople {
    public void importPeople(@PathVariable("sheetId") String sheetId, @Val ImportInput input,
            ServerHttpConnection connection, PersonRepository repository, Audit audit, Rollup rollup,
            Out<ImportResult> result) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        if (!repository.sheetExists(id)) {
            throw new BadRequestException("no such sheet");
        }
        int n = 0;
        for (PersonRequest person : input.people()) {
            n++;
            try {
                NewPerson.checkFree(repository, id, person);
            } catch (BadRequestException e) {
                throw new BadRequestException("line " + n + ": " + e.getMessage());
            }
        }
        for (PersonRequest person : input.people()) {
            NewPerson.add(repository, audit, rollup, id, by, person);
        }
        result.set(new ImportResult(input.people().size()));
    }
}
