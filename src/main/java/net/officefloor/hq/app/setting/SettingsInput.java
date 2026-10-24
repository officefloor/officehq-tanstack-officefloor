package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated settings form: whether the clerk limit is set and to what (two decimals; null: no limit), and
 *  whether the floor is set and to what (two decimals), and whether the terms are set and to what (days), and whether the late percent is set and to what (two decimals),
 *  and whether the policy date is set and to what (null: the sheet's today). */
public record SettingsInput(boolean setClerkLimit, BigDecimal clerkLimit, boolean setFloor, BigDecimal floor,
        boolean setTerms, int terms, boolean setLatePct, BigDecimal latePct,
        boolean setPolicyDate, LocalDate policyDate) {
}
