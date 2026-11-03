package net.officefloor.hq.app.note;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.settings.SettingsRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

/** Add a note to the sheet, dated the sheet's today and by the signed-in user. */
public class SaveNote {
    public void save(@PathVariable("sheetId") String sheetId, @Val String note, ServerHttpConnection connection,
            NoteRepository repository, SettingsRepository settings, Audit audit, Out<Notes> notes)
            throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        if (!repository.sheetExists(id)) {
            throw new BadRequestException("no such sheet");
        }
        repository.add(id, settings.today(id), by, note);
        audit.record("NOTE_ADDED sheet=" + id + " by=" + by + " text=" + note);
        notes.set(new Notes(repository.list(id)));
    }
}
