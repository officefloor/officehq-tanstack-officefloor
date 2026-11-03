package net.officefloor.hq.app.note;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.history.AsOfView;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's notes, newest first; as of a date, those added while the sheet's today was on or before it. */
public class LoadNotes {
    public void load(@PathVariable("sheetId") String sheetId, ServerHttpConnection connection,
            NoteRepository repository, AsOfView view, Out<Notes> notes) throws BadRequestException {
        CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        notes.set(view.read(connection, id, () -> new Notes(repository.list(id))));
    }
}
