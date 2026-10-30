package net.officefloor.hq.app.note;

import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** A note needs some text (a blank one is refused); a viewer, or any user on an archived sheet, is refused. */
public class ValidateNote {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody NoteRequest request, Out<ValidNote> body,
            Settings settings) throws InvalidNoteException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        String text = request.text() == null ? "" : request.text().trim();
        if (text.isEmpty()) {
            throw new InvalidNoteException("A note cannot be blank");
        }
        body.set(new ValidNote(text, request.user()));
    }
}
