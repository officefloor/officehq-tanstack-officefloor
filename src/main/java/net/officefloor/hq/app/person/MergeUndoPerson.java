package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.settings.SettingsRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

/** The steward who made the last merge into a person undoes it on the same sheet-today it was made: the merged
 *  person is back at the end of the list with the claims that came from them (their numbers and status as they are
 *  now) and their own aliases, which leave the other's; patron and pool changes stay (see
 *  {@link MergeRepository#undo}). Writes PERSON_MERGE_UNDONE and the rollup lines. Refused for anyone but the
 *  steward who made it, on any later day, and for a person with no merge to undo. */
public class MergeUndoPerson {
    public void undo(@PathVariable("sheetId") String sheetId, @PathVariable("personId") String personId,
            ServerHttpConnection connection, PersonRepository people, MergeRepository merges,
            SettingsRepository settings, SheetRepository sheets, Audit audit, Rollup rollup, Out<Person> person)
            throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        long pid;
        try {
            pid = Long.parseLong(personId == null ? "" : personId.trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException("no such person");
        }
        Person into = people.find(id, pid);
        if (into == null) {
            throw new BadRequestException("no such person");
        }
        if (!settings.role(id, by).equals(SettingsRepository.STEWARD)) {
            throw new BadRequestException("only a steward can undo a merge");
        }
        MergeRepository.Merge merge = merges.last(id, pid);
        if (merge == null) {
            throw new BadRequestException(into.name() + " has no merge to undo");
        }
        if (!merge.by().equals(by)) {
            throw new BadRequestException("only " + merge.by() + ", who made the merge, can undo it");
        }
        if (!merge.on().equals(sheets.today(id))) {
            throw new BadRequestException("a merge can only be undone on the day it was made");
        }
        String name = merges.name(merge);
        if (merges.namesTaken(id, pid, merge)) {
            throw new BadRequestException("a name of " + name + " is now another person's");
        }
        Rollup.Snapshot before = rollup.take(id);
        merges.undo(id, pid, merge);
        audit.record("PERSON_MERGE_UNDONE sheet=" + id + " by=" + by + " name=" + name + " from=" + into.name());
        rollup.record(id, by, before);
        person.set(people.find(id, merge.fromId()));
    }
}
