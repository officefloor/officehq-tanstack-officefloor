package net.officefloor.hq.app.setting;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class ListPolicyVersions {
    public void list(@PathVariable("sheetId") String sheetId, SettingRepository settings,
            ObjectResponse<List<PolicyVersion>> response) {
        response.send(settings.policyVersions(sheetId.trim()));
    }
}
