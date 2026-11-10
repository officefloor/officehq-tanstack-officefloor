package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateTransfer {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody TransferRequest request,
            Out<TransferInput> body) throws InvalidPersonException {
        String to = request == null || request.toSheet() == null ? "" : request.toSheet().trim();
        if (to.isEmpty()) throw new InvalidPersonException("Give the id of the sheet to transfer to");
        if (to.equals(sheetId == null ? "" : sheetId.trim())) {
            throw new InvalidPersonException("A person cannot be transferred to their own sheet");
        }
        body.set(new TransferInput(to));
    }
}
