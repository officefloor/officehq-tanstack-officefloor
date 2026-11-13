package net.officefloor.hq.app.setting;

import jakarta.validation.constraints.Size;
import java.util.Map;

/**
 * The settings as the form submits them; a setting left out (null) is left as it is. The clerk limit has two
 * decimals at most (blank: no limit); the floor has two decimals at most (blank: back to 1.00); the terms are
 * whole days, 0 or more (blank: back to 14); the late percent has two decimals at most (blank: back to 0.00); the policy date is
 * yyyy-MM-dd (blank: the sheet's today); the period numbering from date is yyyy-MM-dd (blank: off); the cap has two decimals at most (blank: no cap). The grade terms are
 * whole days, 0 or more, by grade, each only for a grade it carries (blank: none of its own, the sheet's terms).
 */
public record SettingsRequest(@Size(max = 30) String clerkLimit, @Size(max = 30) String floor, @Size(max = 30) String terms,
        @Size(max = 30) String latePct, @Size(max = 30) String policyDate, @Size(max = 30) String periodNumberingFrom, @Size(max = 30) String cap,
        @Size(max = 10) Map<String, @Size(max = 30) String> gradeTerms) {
}
