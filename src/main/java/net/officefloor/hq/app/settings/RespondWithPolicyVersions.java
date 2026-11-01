package net.officefloor.hq.app.settings;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's policy versions: one line per version, oldest first, <version> <from or start>
 *  <key>=<value> <count of claims whose policy version it is> — a claim's policy version being the one in
 *  force on its date (DELETED claims left out). Reads only; records nothing. */
public class RespondWithPolicyVersions {
    public void respond(@PathVariable("sheetId") String sheetId, Settings settings, Claims claims,
            ObjectResponse<List<String>> response) {
        List<Settings.PolicyVersion> versions = settings.policyVersionList(sheetId);
        int[] counts = new int[versions.size() + 1];
        for (Claim claim : claims.onSheet(sheetId)) {
            if (claim.getStatus() == ClaimStatus.DELETED) continue;
            LocalDate date = claim.getDate();
            counts[(int) versions.stream().filter(v -> v.from() == null || !v.from().isAfter(date)).count()]++;
        }
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < versions.size(); i++) {
            Settings.PolicyVersion v = versions.get(i);
            lines.add(v.version() + " " + (v.from() != null ? v.from().toString() : "start") + " " + v.key() + "="
                    + v.value() + " " + counts[i + 1]);
        }
        response.send(lines);
    }
}
