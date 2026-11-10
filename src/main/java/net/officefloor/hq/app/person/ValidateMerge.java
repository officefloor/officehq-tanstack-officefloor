package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateMerge {
    public void validate(@RequestBody MergeRequest request, Out<MergeInput> body)
            throws InvalidPersonException {
        String raw = request == null || request.intoId() == null ? "" : request.intoId().trim();
        try {
            body.set(new MergeInput(Long.parseLong(raw)));
        } catch (NumberFormatException e) {
            throw new InvalidPersonException("Choose a person to merge into");
        }
    }
}
