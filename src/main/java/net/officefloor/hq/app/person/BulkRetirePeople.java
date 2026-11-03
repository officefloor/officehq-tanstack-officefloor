package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashSet;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.settings.SettingsRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Retire the selected people together from a date (blank: the sheet's today), in list order (by their
 *  position), each exactly as the row's retire would: PERSON_RETIRED and its rollup lines. An id that is no
 *  person on the sheet is skipped. A viewer's selection is all refused: no one is retired. An archived sheet
 *  refuses it. */
public class BulkRetirePeople {
    public void retire(@PathVariable("sheetId") String sheetId, @RequestBody BulkRetireRequest request,
            ServerHttpConnection connection, PersonRepository repository, SheetRepository sheets, Audit audit,
            Rollup rollup, SettingsRepository settings, Out<BulkRetireResult> result) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        Set<Long> selected = new LinkedHashSet<>();
        if (request != null && request.personIds() != null) {
            for (Long pid : request.personIds()) {
                if (pid != null) selected.add(pid);
            }
        }
        if (selected.isEmpty()) {
            throw new BadRequestException("select at least one person");
        }
        String raw = request.date() == null ? "" : request.date().trim();
        LocalDate from;
        try {
            from = raw.isEmpty() ? sheets.today(id) : LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("date must be yyyy-mm-dd");
        }
        if (settings.archived(id)) {
            throw new BadRequestException("the sheet is archived");
        }
        int retired = 0;
        if (settings.role(id, by).equals(SettingsRepository.VIEWER)) {
            result.set(new BulkRetireResult(retired));
            return;
        }
        for (Person current : repository.list(id)) {
            if (!selected.contains(current.id())) continue;
            Rollup.Snapshot before = rollup.take(id);
            repository.retire(id, current.id(), from);
            audit.record("PERSON_RETIRED sheet=" + id + " by=" + by + " name=" + current.name() + " from=" + from);
            rollup.record(id, by, before);
            retired++;
        }
        result.set(new BulkRetireResult(retired));
    }
}
