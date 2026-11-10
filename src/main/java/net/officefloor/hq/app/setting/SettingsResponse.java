package net.officefloor.hq.app.setting;

/**
 * A sheet's settings, as the screen reads them: the clerk limit with two decimals ('' for none), the
 * floor with two decimals, the terms in days and the late percent with two decimals, and the policy date as yyyy-MM-dd ('' for
 * the sheet's today), and the period numbering from as yyyy-MM-dd ('' for off), and each grade's own
 * terms in days ('' for the sheet's terms).
 */
public record SettingsResponse(String clerkLimit, String floor, String terms, String latePct, String policyDate,
        String periodNumberingFrom, java.util.Map<String, String> gradeTerms) {
    public static SettingsResponse from(Settings settings) {
        return new SettingsResponse(settings.clerkLimit() == null ? "" : settings.clerkLimit().toPlainString(),
                settings.floor().setScale(2).toPlainString(), Integer.toString(settings.terms()),
                settings.latePct().setScale(2).toPlainString(),
                settings.policyDate() == null ? "" : settings.policyDate().toString(),
                settings.periodNumberingFrom() == null ? "" : settings.periodNumberingFrom().toString(),
                gradeTerms(settings));
    }

    private static java.util.Map<String, String> gradeTerms(Settings settings) {
        java.util.Map<String, String> shown = new java.util.LinkedHashMap<>();
        for (String grade : Settings.GRADES) {
            Integer own = settings.gradeTerms().get(grade);
            shown.put(grade, own == null ? "" : own.toString());
        }
        return shown;
    }
}
