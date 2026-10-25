package net.officefloor.hq.app;

import java.util.Map;
import net.officefloor.plugin.section.clazz.Parameter;
import net.officefloor.web.ObjectResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class HandleConflict {
    public void handle(@Parameter ConflictException ex,
            ObjectResponse<ResponseEntity<Map<String, String>>> response) {
        response.send(new ResponseEntity<>(Map.of("error", ex.getMessage()), HttpStatus.CONFLICT));
    }
}
