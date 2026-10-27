package net.officefloor.hq.app.template;

import net.officefloor.plugin.section.clazz.Parameter;
import net.officefloor.web.ObjectResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class HandleTemplateRefused {
    public void handle(@Parameter TemplateRefusedException ex, ObjectResponse<ResponseEntity<String>> response) {
        response.send(new ResponseEntity<>(ex.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY));
    }
}
