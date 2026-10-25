package net.officefloor.hq.app.person;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Whether to start or stop watching, with the signed-in user doing it. */
public class ValidateWatch {
    public void validate(@RequestBody WatchRequest request, Out<WatchInput> watch, Out<ChangedBy> changedBy)
            throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        if (request.on() == null) {
            throw new BadRequestException("Say whether to watch or stop watching");
        }
        watch.set(new WatchInput(request.on()));
        changedBy.set(by);
    }
}
