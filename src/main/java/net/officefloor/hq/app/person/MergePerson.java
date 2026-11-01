package net.officefloor.hq.app.person;

import net.officefloor.hq.app.settings.Role;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** A steward merges the person into another on the sheet (see {@link People#merge}). Both are read
 *  afresh and locked. Refused: anyone but a steward, the person themselves, and either of them
 *  retired. Publishes the person merged into, as they are now. */
public class MergePerson {
    public void merge(@PathVariable("sheetId") String sheetId, @PathVariable("personId") long personId,
            @Val MergeRequest request, People people, Settings settings, Sheets sheets, Out<Person> personOut,
            Out<Merger> mergerOut) throws PersonNotFoundException, InvalidSheetRequestException {
        if (settings.role(sheetId, request.user()) != Role.STEWARD) {
            throw new InvalidSheetRequestException("Only a steward can merge people");
        }
        if (request.intoId() == personId) {
            throw new InvalidSheetRequestException("A person cannot be merged into themselves");
        }
        Person from = people.lock(sheetId, personId);
        if (from == null) throw new PersonNotFoundException(personId);
        Person into = people.lock(sheetId, request.intoId());
        if (into == null) throw new InvalidSheetRequestException("No such person on this sheet");
        if (from.isRetired() || into.isRetired()) {
            throw new InvalidSheetRequestException("A retired person cannot be merged, nor merged into");
        }
        people.merge(from, into, request.user(), sheets.today(sheetId));
        Person now = people.find(sheetId, into.getId());
        personOut.set(now);
        mergerOut.set(new Merger(from, now, request.user()));
    }
}
