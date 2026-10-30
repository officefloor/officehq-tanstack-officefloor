package net.officefloor.hq.app.statement;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateStatement {
    public void validate(@RequestBody StatementRequest request, Out<StatementRequest> body) {
        body.set(request);
    }
}
