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

import java.util.ArrayList;
import java.util.List;

/** A steward transfers a person to another sheet by id: they arrive there with every grade they were given (and
 *  its from-date), their terms, contact and aliases, and their claims (see {@link ClaimRepository#transferAll}), with
 *  no statements; they leave this sheet entirely (patron links, pool memberships, holds and watches dropped).
 *  A sheet id never used is made empty first, as loading it would. Refused for anyone but a steward, for a retired
 *  person, for this sheet's own id, and for an archived sheet. When their name or an alias is already a name or alias
 *  of someone on the other sheet, they are merged into that person there, as {@link MergePerson} does (claims after
 *  theirs, aliases joined, bound figures lost; refused when that person is retired or more than one person matches);
 *  otherwise refused when their name, an alias or their contact is already taken there. Each sheet writes its
 *  PERSON_TRANSFERRED line (the other sheet then PERSON_MERGED when merged), then its rollup lines. */
public class TransferPerson {
    public void transfer(@PathVariable("sheetId") String sheetId, @PathVariable("personId") String personId,
            @RequestBody TransferRequest request, ServerHttpConnection connection, PersonRepository people,
            AliasRepository aliases, ClaimRepository claims, SettingsRepository settings, SheetRepository sheets, Audit audit,
            Rollup rollup,
            Out<Person> person) throws BadRequestException {
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
            throw new BadRequestException("only a steward can transfer people");
        }
        if (from.retired()) {
            throw new BadRequestException(from.name() + " is retired");
        }
        String to = request == null || request.toSheetId() == null ? "" : request.toSheetId().trim();
        if (to.isEmpty()) {
            throw new BadRequestException("enter the sheet to transfer to");
        }
        if (to.equals(id)) {
            throw new BadRequestException("a person cannot be transferred to their own sheet");
        }
        if (settings.archived(to)) {
            throw new BadRequestException("sheet " + to + " is archived");
        }
        List<String> names = new ArrayList<>(List.of(from.name()));
        for (Alias alias : aliases.list(id)) {
            if (alias.personId() == pid) names.add(alias.alias());
        }
        Person into = null;
        for (String name : names) {
            Person match = people.findByName(to, name);
            if (match == null) continue;
            if (into != null && into.id() != match.id()) {
                throw new BadRequestException(name + " is already on sheet " + to);
            }
            into = match;
        }
        if (into != null) {
            if (into.retired()) {
                throw new BadRequestException(into.name() + " is retired");
            }
        } else {
            for (String name : names) {
                if (people.nameTaken(to, name, pid)) {
                    throw new BadRequestException(name + " is already on sheet " + to);
                }
            }
            if (from.contact() != null && people.contactTaken(to, from.contact(), 0)) {
                throw new BadRequestException(from.contact() + " is already on sheet " + to);
            }
        }

        if (!people.sheetExists(to)) {
            sheets.loadOrCreate(to, by);
        }
        Rollup.Snapshot beforeHere = rollup.take(id);
        Rollup.Snapshot beforeThere = rollup.take(to);
        long arrived;
        if (into == null) {
            arrived = people.transferIn(id, pid, to);
            claims.transferAll(id, pid, to, arrived);
        } else {
            // They arrive as merged into the person already there: their claims after that person's, their name
            // and aliases joined to that person's aliases.
            arrived = into.id();
            claims.transferAll(id, pid, to, arrived);
            people.joinAliases(to, arrived, names);
        }
        people.remove(id, pid);
        audit.record("PERSON_TRANSFERRED sheet=" + id + " by=" + by + " name=" + from.name() + " to=" + to);
        rollup.record(id, by, beforeHere);
        audit.record("PERSON_TRANSFERRED sheet=" + to + " by=" + by + " name=" + from.name() + " from=" + id);
        if (into != null) {
            audit.record("PERSON_MERGED sheet=" + to + " by=" + by + " from=" + from.name() + " into=" + into.name());
        }
        rollup.record(to, by, beforeThere);
        person.set(people.find(to, arrived));
    }
}
