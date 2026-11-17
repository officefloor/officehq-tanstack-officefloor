package net.officefloor.hq.app.note;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class RespondWithNoteCreated {
    public void respond(@Val Note note, ObjectResponse<ResponseEntity<Note>> response) {
        response.send(new ResponseEntity<>(note, HttpStatus.CREATED));
    }
}
