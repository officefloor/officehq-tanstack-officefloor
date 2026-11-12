package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateProof {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody ProofRequest request, Out<ProofInput> body, RoleStore roles) throws InvalidSheetException {
        roles.refuseChange(sheetId.trim(), request == null ? null : request.user());
        String kind = request == null || request.kind() == null ? "" : request.kind().trim();
        if (!kind.isEmpty() && !SettingStore.KINDS.contains(kind)) {
            throw new InvalidSheetException("A proof is a receipt or a photo");
        }
        body.set(new ProofInput(request == null ? null : request.user(), kind.isEmpty() ? null : kind));
    }
}
