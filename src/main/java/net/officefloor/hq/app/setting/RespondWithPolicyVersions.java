package net.officefloor.hq.app.setting;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's policy versions, numbered from 1 in the order made, each with its from-date (null: none, counts
 *  always); read as of a date (see AsOf), only those made while the sheet's today was on or before it. */
public class RespondWithPolicyVersions {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "asOf", required = false) String asOf, SettingRepository settings,
            ObjectResponse<List<PolicyVersion>> response) {
        String sheet = sheetId.trim();
        LocalDate date = AsOf.parse(asOf);
        List<LocalDate> froms = date == null ? settings.policyVersions(sheet)
                : SettingRepository.policyVersions(settings.storedBy(sheet, date));
        List<PolicyVersion> versions = new ArrayList<>();
        for (int i = 0; i < froms.size(); i++) {
            versions.add(new PolicyVersion(i + 1, froms.get(i)));
        }
        response.send(versions);
    }

    /** A policy version: its number and the date it is in force from (null: none). */
    public record PolicyVersion(int number, LocalDate from) {
    }
}
