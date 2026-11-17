package net.officefloor.hq.app.setting;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's policy versions, numbered from 1 in the order made, each with the setting it changed, its value, its
 *  from-date (null: none) and its start (its from-date, or the sheet's today when made); read as of a date (see AsOf),
 *  only those made while the sheet's today was on or before it. */
public class RespondWithPolicyVersions {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "asOf", required = false) String asOf, SettingRepository settings,
            ObjectResponse<List<PolicyVersion>> response) {
        List<SettingRepository.PolicyVersion> stored = settings.policyVersions(sheetId.trim(), AsOf.parse(asOf));
        List<PolicyVersion> versions = new ArrayList<>();
        for (int i = 0; i < stored.size(); i++) {
            SettingRepository.PolicyVersion v = stored.get(i);
            versions.add(new PolicyVersion(i + 1, v.key(), v.value(), v.from(), v.start()));
        }
        response.send(versions);
    }

    /** A policy version: its number, the setting and value, its from-date (null: none) and the date it starts. */
    public record PolicyVersion(int number, String key, String value, LocalDate from, LocalDate start) {
    }
}
