package net.officefloor.hq.app.note;

import net.officefloor.hq.app.setting.InvalidSettingException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Refused for a blank note. */
public class ValidateNote {
    public void validate(@RequestBody NoteRequest request, Out<NoteRequest> body) throws InvalidSettingException {
        String text = request == null || request.text() == null ? "" : request.text().trim();
        if (text.isEmpty()) {
            throw new InvalidSettingException("A note needs text");
        }
        body.set(new NoteRequest(text));
    }
}
