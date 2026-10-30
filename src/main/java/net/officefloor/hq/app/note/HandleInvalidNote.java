package net.officefloor.hq.app.note;

import net.officefloor.plugin.section.clazz.Parameter;
import net.officefloor.web.ObjectResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class HandleInvalidNote {
    public void handle(@Parameter InvalidNoteException ex, ObjectResponse<ResponseEntity<String>> response) {
        response.send(new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST));
    }
}
