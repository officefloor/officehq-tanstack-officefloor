package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.settings.SettingsRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** A steward merges a person into another: the merged person's claims move to the other (numbers and dates
 *  kept, listed after the other's in the order they were added, each keeping any figure a statement bound), their
 *  name and aliases become the other's aliases, their patrons, the people naming them and their pool memberships
 *  pass to the other, and they leave the sheet (their statements joined to the other's — the other's count becomes their own plus
 *  the merged person's, the other's last statement staying theirs, or becoming the merged person's when they had
 *  none; holds, watchers and contact dropped). The merge is kept as the last into the other, so the steward can undo it
 *  that sheet-today (see {@link MergeUndoPerson}). Refused for anyone
 *  but a steward, for the same person, and when either of them is retired. */
public class MergePerson {
    public void merge(@PathVariable("sheetId") String sheetId, @PathVariable("personId") String personId,
            @RequestBody MergeRequest request, ServerHttpConnection connection, PersonRepository people,
            ClaimRepository claims, SettingsRepository settings, SheetRepository sheets, MergeRepository merges, Audit audit, Rollup rollup, Out<Person> person)
            throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        long pid;
        try {
            pid = Long.parseLong(personId == null ? "" : personId.trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException("no such person");
        }
        Person from = people.find(id, pid);
        if (from == null) {
            throw new BadRequestException("no such person");
        }
        if (!settings.role(id, by).equals(SettingsRepository.STEWARD)) {
            throw new BadRequestException("only a steward can merge people");
        }
        Person into = request == null || request.intoId() == null ? null : people.find(id, request.intoId());
        if (into == null) {
            throw new BadRequestException("choose the person to merge into");
        }
        if (into.id() == pid) {
            throw new BadRequestException("a person cannot be merged into themselves");
        }
        if (from.retired()) {
            throw new BadRequestException(from.name() + " is retired");
        }
        if (into.retired()) {
            throw new BadRequestException(into.name() + " is retired");
        }
        Rollup.Snapshot before = rollup.take(id);
        merges.keep(id, pid, into.id(), by, sheets.today(id));
        claims.moveAll(id, pid, into.id());
        people.merge(id, pid, into.id());
        audit.record("PERSON_MERGED sheet=" + id + " by=" + by + " from=" + from.name() + " into=" + into.name());
        rollup.record(id, by, before);
        person.set(people.find(id, into.id()));
    }
}
