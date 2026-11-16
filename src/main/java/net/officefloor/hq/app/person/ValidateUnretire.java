package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: who unretires the person. */
public class ValidateUnretire {
    public void validate(@RequestBody UnretireRequest request, Out<Unretirement> unretirement) {
        unretirement.set(new Unretirement(request == null || request.by() == null ? "" : request.by().trim()));
    }
}
