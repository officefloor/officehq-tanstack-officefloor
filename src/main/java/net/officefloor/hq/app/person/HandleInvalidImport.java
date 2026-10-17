package net.officefloor.hq.app.person;

import net.officefloor.plugin.section.clazz.Parameter;
import net.officefloor.web.ObjectResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class HandleInvalidImport {
    public void handle(@Parameter InvalidImportException ex, ObjectResponse<ResponseEntity<ImportResponse>> response) {
        response.send(new ResponseEntity<>(new ImportResponse(0, ex.getLines()), HttpStatus.BAD_REQUEST));
    }
}
