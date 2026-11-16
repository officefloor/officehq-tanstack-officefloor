package net.officefloor.hq.app.statement;

import net.officefloor.web.ObjectResponse;
import net.officefloor.plugin.variable.Val;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class RespondWithStatementCreated {
    public void respond(@Val Statement statement, ObjectResponse<ResponseEntity<Statement>> response) {
        response.send(new ResponseEntity<>(statement, HttpStatus.CREATED));
    }
}
