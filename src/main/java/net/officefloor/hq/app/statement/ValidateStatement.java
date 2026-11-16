package net.officefloor.hq.app.statement;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: who takes the statement. */
public class ValidateStatement {
    public void validate(@RequestBody StatementRequest request, Out<StatementTaker> taker) {
        taker.set(new StatementTaker(request == null || request.by() == null ? "" : request.by().trim()));
    }
}
