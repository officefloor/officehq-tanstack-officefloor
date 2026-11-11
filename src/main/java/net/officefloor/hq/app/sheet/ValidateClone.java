package net.officefloor.hq.app.sheet;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateClone {
    public void validate(@RequestBody CloneRequest request, Out<CloneInput> body) throws InvalidSheetIdException {
        String to = request == null || request.toSheet() == null ? "" : request.toSheet().trim();
        if (to.isEmpty()) throw new InvalidSheetIdException("Give the id of the sheet to clone to");
        body.set(new CloneInput(to));
    }
}
