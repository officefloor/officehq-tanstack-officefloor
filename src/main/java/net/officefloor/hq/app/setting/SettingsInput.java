package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/** A validated settings save: who is making it, and the settings it changes, in order — whether it sets the clerk
 *  limit and to what (two decimals; null: no limit), and the floor it sets (two decimals; null: left as it is) and the terms it sets (days; null: left as it is) and the late percent it sets (two decimals; null: left as it is), and whether it sets the policy date and to what (null: blank, the sheet's today), and whether it sets the period numbering from and to what (null: blank, off), and the grades whose own terms it sets
 *  (A, B, C order) and to what (days; null: none, the sheet's terms), and whether it sets the cap and to what (two
 *  decimals; null: none), and the from-dates given for the dated ones it sets, by key (one not given: from the sheet's
 *  today), and whether it sets the proof threshold and to what (two decimals; null: none), and the permission entries it sets
 *  (in action order) and to what (roles comma separated; '': the standing rule), and whether it sets the grace days and to what (days; null: off), and whether it sets the note threshold and to what (two decimals; null: none), and whether it sets the entry date and to what (null: blank, off), and the grades whose grace it sets (A, B, C order) and to what (days; null: none), and the grades whose own proof threshold it sets (A, B, C order) and to what (two decimals; null: none, the sheet's), and whether it sets the pot and to what (two decimals; null: none), and whether it sets the parent sheet and to what (a sheet id; null: none), and the fiscal year start month it sets (1 to 12; null: left as it is), and whether it sets the alert threshold and to what (two decimals; null: off). */
public record SettingsInput(String user, boolean setsClerkLimit, BigDecimal clerkLimit, BigDecimal floor,
        Integer terms, BigDecimal latePct, boolean setsPolicyDate, LocalDate policyDate,
        boolean setsPeriodNumberingFrom, LocalDate periodNumberingFrom,
        Map<String, Integer> gradeTerms, boolean setsCap, BigDecimal cap,
        Map<String, LocalDate> from, boolean setsProofThreshold, BigDecimal proofThreshold,
        Map<String, String> perms, boolean setsGraceDays, Integer graceDays,
        boolean setsNoteThreshold, BigDecimal noteThreshold,
        boolean setsEntryDate, LocalDate entryDate, Map<String, Integer> gradeGrace,
        Map<String, BigDecimal> gradeProofThreshold, boolean setsPot, BigDecimal pot,
        boolean setsParent, String parent, Integer fiscalStartMonth, boolean setsAlertThreshold, BigDecimal alertThreshold) {
}
