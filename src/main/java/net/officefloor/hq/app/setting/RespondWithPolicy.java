package net.officefloor.hq.app.setting;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.AsOf;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/**
 * The sheet's policy versions, each with the count of claims whose policy version it is: a claim's policy
 * version is the number of versions whose from-date is on or before its date (a version with none counting
 * always), as the screen numbers it. A DELETED claim counts nowhere.
 */
public class RespondWithPolicy {
    public void respond(@Val Sheet sheet, SettingsRepository repository, ClaimRepository claims,
            HttpServletRequest request, ObjectResponse<PolicyResponse> response) {
        List<SettingsRepository.Version> versions = repository.versionChanges(sheet.id(), AsOf.of(request));
        int[] counts = new int[versions.size() + 1];
        for (Claim claim : claims.findAll(sheet.id())) {
            if (claim.getStatus() == ClaimStatus.DELETED) continue;
            counts[versionOn(versions, claim.getDate())]++;
        }
        List<PolicyResponse.Line> lines = new ArrayList<>();
        for (int i = 0; i < versions.size(); i++) {
            SettingsRepository.Version v = versions.get(i);
            lines.add(new PolicyResponse.Line(i + 1, v.from() == null ? "" : v.from().toString(), v.key(),
                    v.value() == null ? "" : v.value(), counts[i + 1]));
        }
        response.send(new PolicyResponse(lines));
    }

    private static int versionOn(List<SettingsRepository.Version> versions, LocalDate date) {
        int n = 0;
        for (SettingsRepository.Version v : versions) {
            if (v.from() == null || (date != null && !v.from().isAfter(date))) n++;
        }
        return n;
    }
}
