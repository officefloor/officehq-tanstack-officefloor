package net.officefloor.hq.app.setting;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Every known setting of the sheet, blank ones included; the floor, the terms and the late percent show their defaults until it is set; a grade's terms are blank until set; a blank policy date is blank. */
public class RespondWithSettings {
    public void respond(@PathVariable("sheetId") String sheetId, SettingRepository repository,
            ObjectResponse<List<SettingResponse>> response) {
        response.send(List.of(new SettingResponse(ClerkLimit.KEY, repository.find(sheetId, ClerkLimit.KEY).orElse("")),
                new SettingResponse(Floor.KEY, repository.find(sheetId, Floor.KEY).orElse(Floor.DEFAULT)),
                new SettingResponse(Terms.KEY, repository.find(sheetId, Terms.KEY).orElse(Terms.DEFAULT)),
                new SettingResponse(Terms.keyOf("A"), repository.find(sheetId, Terms.keyOf("A")).orElse("")),
                new SettingResponse(Terms.keyOf("B"), repository.find(sheetId, Terms.keyOf("B")).orElse("")),
                new SettingResponse(Terms.keyOf("C"), repository.find(sheetId, Terms.keyOf("C")).orElse("")),
                new SettingResponse(LatePct.KEY, repository.find(sheetId, LatePct.KEY).orElse(LatePct.DEFAULT)),
                new SettingResponse(PolicyDate.KEY, repository.find(sheetId, PolicyDate.KEY).orElse("")),
                new SettingResponse(PeriodNumberingFrom.KEY, repository.find(sheetId, PeriodNumberingFrom.KEY).orElse(""))));
    }
}
