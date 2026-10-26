/** A dated setting's value (as the settings show it) recorded from `from` (yyyy-mm-dd). */
export type DatedValue = { from: string; value: string };

/** The floor a sheet starts with: in force before its first from-date. */
export const DEFAULT_FLOOR = '1.00';

/** The late percent a sheet starts with: in force before its first from-date. */
export const DEFAULT_LATE_PCT = '0.00';

/** The terms (days) a sheet starts with: in force before their first from-date. */
export const DEFAULT_TERMS = '14';

/** The value in force on the date (yyyy-mm-dd): the one with the latest from-date on or before it, or undefined when none is. */
export function valueOn(values: DatedValue[] | undefined, date: string): string | undefined {
  const applying = (values ?? []).filter((v) => v.from <= date);
  return applying.reduce<DatedValue | undefined>((best, v) => (!best || v.from > best.from ? v : best), undefined)?.value;
}
