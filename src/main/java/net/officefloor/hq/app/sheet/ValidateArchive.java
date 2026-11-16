package net.officefloor.hq.app.sheet;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: who archives the sheet. */
public class ValidateArchive {
    public void validate(@RequestBody ArchiveRequest request, Out<ArchiveRequest> body) {
        body.set(new ArchiveRequest(request == null || request.by() == null ? "" : request.by().trim()));
    }
}
