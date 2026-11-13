package net.officefloor.hq.app.sheet;

import jakarta.validation.Valid;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateSheetRequest {
    public void validate(@RequestBody @Valid SheetRequest request, Out<SheetRequest> body) {
        body.set(new SheetRequest(request.id().trim()));
    }
}
