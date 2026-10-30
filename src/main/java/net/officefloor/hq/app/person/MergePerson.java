package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.RequestBody;

public class MergePerson {
    // Only a steward (a delegation included) merges a person, into another person on the sheet; neither may be retired.
    public void merge(@RequestBody(required = false) MergeRequest body, @Val Person person, People people,
            Settings settings, Sheets sheets, ServerHttpConnection connection, Out<MergedPerson> mergedOut,
            Out<PersonResponse> responseOut) throws PersonMergeRefusedException {
        if (!Settings.STEWARD.equals(settings.role(person.sheetId(), Audit.user(connection).trim()))) {
            throw new PersonMergeRefusedException("Only a steward can merge people.");
        }
        Person into = body == null || body.intoId() == null ? null : people.find(person.sheetId(), body.intoId());
        if (into == null) {
            throw new PersonMergeRefusedException("Choose a person on this sheet to merge into.");
        }
        if (into.id() == person.id()) {
            throw new PersonMergeRefusedException("A person cannot be merged into themselves.");
        }
        if (person.retired() || into.retired()) {
            throw new PersonMergeRefusedException("A retired person cannot be merged.");
        }
        people.merge(person.sheetId(), person.id(), into.id(), Audit.user(connection).trim(), sheets.today(person.sheetId()));
        mergedOut.set(new MergedPerson(person, into));
        responseOut.set(PersonResponse.from(people.find(person.sheetId(), into.id()), sheets.today(person.sheetId())));
    }
}
