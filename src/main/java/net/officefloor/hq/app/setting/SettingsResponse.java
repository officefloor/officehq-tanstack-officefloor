package net.officefloor.hq.app.setting;

import java.util.Map;

/** The sheet's settings as the front end reads them: {@code clerkLimit} has two decimals, null for
 *  no limit; {@code floor} has two decimals; {@code terms} is the whole days after its date a claim is
 *  due by;
 *  {@code latePct} has two decimals; {@code policyDate} is the date rounding turns on, null when blank
 *  (then the sheet's today); {@code periodNumberingFrom} is the date from which claims opened are
 *  numbered per period, null when blank (off); {@code gradeTerms} is the terms in force for each grade
 *  (its own, else the sheet's), by grade; {@code cap} has two decimals, the most a person's due-total may
 *  be, null for no cap. */
public record SettingsResponse(String clerkLimit, String floor, String terms, String latePct, String policyDate,
        String periodNumberingFrom, Map<String, String> gradeTerms, String cap) {
}
