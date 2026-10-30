import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { readPath, useAsOf } from '../asof/useAsOf';

/** A grade's rate: a percent shown with two decimals ("12.50"), in force from a date (YYYY-MM-DD;
 *  "" for from the beginning of time) through a date ("" for no end), and its version: how many times a
 *  rate has been set for the grade and from-date, this the newest. One inherited from an ancestor
 *  sheet has its level: 1 the parent, 2 the parent's parent, 3 the one above; the sheet's own have none.
 *  afterMark is 0 for a rate applying to every claim; a forward-only one (saved with a from-date on or after the
 *  sheet's today) applies only to claims whose rateMark is greater, the others falling back to the older versions
 *  it replaced (earlier, newest first). */
export type Rate = {
  grade: string;
  rate: string;
  from: string;
  to: string;
  version: number;
  level?: number;
  afterMark?: number;
  earlier?: { rate: string; to: string; afterMark: number }[];
};

/** Whether a rate (version) with this mark applies to a claim with that one; no claim mark: the rate in force,
 *  without regard to forward-only rates (as the rates panel, the rate preview and the rate calendar show it). */
function applies(afterMark: number | undefined, claimMark: number | undefined): boolean {
  return !afterMark || claimMark === undefined || claimMark > afterMark;
}

export const ratesKey = (sheet: string, asOf = '') => ['sheet', sheet, 'rates', asOf] as const;

export const inheritedRatesKey = (sheet: string, asOf = '') => ['sheet', sheet, 'inherited-rates', asOf] as const;

/** The sheet's own rates, by grade then from-date (from the beginning of time first), followed by
 *  its ancestors' (nearest first), each with its level. */
export function useRates(sheet: string): Rate[] {
  const asOf = useAsOf();
  const { data } = useQuery({
    queryKey: ratesKey(sheet, asOf),
    queryFn: () => getJson<Rate[]>(readPath(`/api/sheets/${encodeURIComponent(sheet)}/rates`, asOf)),
    enabled: sheet !== '',
  });
  const { data: inherited } = useQuery({
    queryKey: inheritedRatesKey(sheet, asOf),
    queryFn: () => getJson<Rate[][]>(readPath(`/api/sheets/${encodeURIComponent(sheet)}/inherited-rates`, asOf)),
    enabled: sheet !== '',
  });
  return useMemo(
    () => [...(data ?? []), ...(inherited ?? []).flatMap((rates, i) => rates.map((r) => ({ ...r, level: i + 1 })))],
    [data, inherited],
  );
}

/** One sheet's grade rate in force on a date (for a claim with a mark: of the versions that apply to it): the one with
 *  the latest from-date on or before it; undefined when none applies, or when that one's to-date is before the date. */
function ownRateOn(rates: Rate[], grade: string, date: string, level: number, mark?: number): string | undefined {
  let found: { from: string; rate: string; to: string } | undefined;
  for (const r of rates) {
    if ((r.level ?? 0) !== level || r.grade !== grade || r.from > date || (found !== undefined && r.from < found.from)) continue;
    const version = [r, ...(r.earlier ?? [])].find((v) => applies(v.afterMark, mark));
    if (version !== undefined) found = { from: r.from, rate: version.rate, to: version.to };
  }
  if (found === undefined || (found.to !== '' && found.to < date)) return undefined;
  return found.rate;
}

/** The grade's rate in force on a date: the sheet's own (see ownRateOn); when it has none, its parent's,
 *  then the parent's parent's, up to three levels up; undefined when none has one (or the date is not known yet). */
export function rateOn(rates: Rate[], grade: string, date: string | undefined, mark?: number): string | undefined {
  if (date === undefined) return undefined;
  for (let level = 0; level <= 3; level++) {
    const found = ownRateOn(rates, grade, date, level, mark);
    if (found !== undefined) return found;
  }
  return undefined;
}

/** The grade's rate that applies to a claim on its date (see rateOn): a forward-only rate only when the claim
 *  arrived on the sheet after it was saved. */
export function claimRate(rates: Rate[], grade: string, claim: { date: string; rateMark?: number }): string | undefined {
  return rateOn(rates, grade, claim.date, claim.rateMark ?? 0);
}
