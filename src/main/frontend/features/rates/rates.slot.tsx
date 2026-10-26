import { useRef, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { getJson, putJson } from '../../api/http';
import { ClaimCells } from '../../slots/defs/claimRow';
import { PersonCells, PersonHeads } from '../../slots/defs/personRow';
import { SheetBody } from '../../slots/defs/sheetBody';
import { claimsKey, counts, useClaimTags, useClaims, type Claim } from '../claims/useClaims';
import { gradeOn, peopleKey, useHeld, usePeople, type Person } from '../people/people.slot';
import { useSheetToday } from '../today/today.slot';
import { useUser } from '../session/useSession';
import { claimDueCents, isBound, isOpen, rateOn, ratesKey, showHundredths, toHundredths, trimmedSum, useRates, type Rate } from './useRates';
import { useSubmitOnce } from '../../mutation/useSubmitOnce';
import { useCapCents, useFloorCents, useLate } from '../access/useAccess';

const GRADES = ['A', 'B', 'C'] as const;
const RATE = /^\d{1,3}(\.\d{1,2})?$/;
export const NA = 'n/a';

/** The rate (in hundredths of a percent) in force on the sheet's today for each grade that has one. */
function useRateByGrade(sheetId: string): Map<string, bigint> {
  const rates = useRates(sheetId);
  const today = useSheetToday(sheetId).data?.today;
  const byGrade = new Map<string, bigint>();
  if (today === undefined) return byGrade;
  for (const g of GRADES) {
    const r = rateOn(rates.data ?? [], g, today);
    if (r) byGrade.set(g, toHundredths(r.rate));
  }
  return byGrade;
}

/** A person's due-total in cents, held to the sheet's cap: the cap when their figures add up to more (an n/a one is
 *  not capped). Everything that adds up due-totals uses this. */
export function useDueTotals(sheetId: string): Map<number, bigint | null> {
  const sums = useUncappedDueTotals(sheetId);
  const cap = useCapCents(sheetId);
  const totals = new Map<number, bigint | null>();
  for (const [id, sum] of sums) totals.set(id, sum != null && cap != null && sum > cap ? cap : sum);
  return totals;
}

/** A person's excess in cents: how far their figures add up to more than the sheet's cap; 0 otherwise (and when n/a). */
export function useExcesses(sheetId: string): Map<number, bigint> {
  const sums = useUncappedDueTotals(sheetId);
  const cap = useCapCents(sheetId);
  const excess = new Map<number, bigint>();
  for (const [id, sum] of sums) excess.set(id, sum != null && cap != null && sum > cap ? sum - cap : 0n);
  return excess;
}

/** A person's uncapped due-total in cents: their counted claims' already-rounded figures added up (0 with none). Null when
 *  their grade has no rate on the sheet's today, unless a counted settled or stated claim keeps a figure, or a claim has
 *  one at a grade other than today's or dated on or before today (from its own date): then the figures there are, added up. */
function useUncappedDueTotals(sheetId: string): Map<number, bigint | null> {
  const parts = useDueParts(sheetId);
  const totals = new Map<number, bigint | null>();
  for (const [id, part] of parts) totals.set(id, part.total);
  return totals;
}

type Split = { settled: bigint; open: bigint };

/** A person's settled-total and open-total in cents: the figures of their counted SETTLED claims (corrections included), and of
 *  their counted OPEN and PENDING ones, each added up (n/a as 0). Together they make the uncapped due-total. */
export function useSplitTotals(sheetId: string): Map<number, Split> {
  const parts = useDueParts(sheetId);
  const splits = new Map<number, Split>();
  for (const [id, part] of parts) splits.set(id, { settled: part.settled, open: part.open });
  return splits;
}

/** The pieces of each person's uncapped due-total: the total itself (null when n/a) and its settled and open parts. */
function useDueParts(sheetId: string): Map<number, Split & { total: bigint | null }> {
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const today = useRateByGrade(sheetId);
  const todayDate = useSheetToday(sheetId).data?.today;
  const held = useHeld(sheetId);
  const floor = useFloorCents(sheetId);
  const late = useLate(sheetId);
  const parts = new Map<number, Split & { total: bigint | null }>();
  for (const p of people.data ?? []) {
    const counted = (claims.data ?? []).filter((c) => c.personId === p.id && counts(c, held));
    let settled = 0n;
    let open = 0n;
    let kept = false;
    for (const c of counted) {
      const grade = gradeOn(p, c.date);
      const due = claimDueCents(rates.data ?? [], grade, c, floor, late);
      // A figure dated on or before today stands even if its rate has ended since.
      kept ||= c.settledDue != null || (isBound(c) && c.statedDue != null)
        || (due != null && (grade !== p.grade || (todayDate !== undefined && c.date <= todayDate)));
      if (c.status === 'SETTLED') settled += due ?? 0n;
      else open += due ?? 0n;
    }
    parts.set(p.id, { settled, open, total: today.has(p.grade) || kept ? settled + open : null });
  }
  return parts;
}

// The rate form: sets a grade's rate from a date (blank: from the beginning of time) through a to-date
// (blank: with no end; never before the from-date); one already set for that grade and date is
// kept as an older version and the new one is in force, the row showing its version count. Every rate is listed under it, with each grade's rate in
// force on the sheet's today.
function RatesPanel({ sheetId }: { sheetId: string; user: string }) {
  const rates = useRates(sheetId);
  const [grade, setGrade] = useState<string>('A');
  const [pct, setPct] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const today = useSheetToday(sheetId).data?.today;
  const [user] = useUser();
  const queryClient = useQueryClient();
  const save = useMutation({
    mutationFn: async (body: { grade: string; rate: string; from: string; to: string }) => {
      if (body.rate.trim() === '') throw new Error('Enter a rate');
      if (!RATE.test(body.rate.trim()) || Number(body.rate) > 100) {
        throw new Error('A rate is a percent, 0.00 to 100.00, with up to two decimals');
      }
      // `by` is the signed-in user the change is recorded against.
      return putJson<Rate>(`/api/sheets/${encodeURIComponent(sheetId)}/rates`, { ...body, from: body.from.trim(), to: body.to.trim(), by: user ?? '' });
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ratesKey(sheetId) });
      setPct('');
      setFrom('');
      setTo('');
    },
  });

  const submit = useSubmitOnce(save);
  return (
    <section data-testid="rates-panel">
      <h2>Rates</h2>
      <form
        data-testid="rate-form"
        onSubmit={(e) => {
          e.preventDefault();
          submit({ grade, rate: pct, from, to });
        }}
      >
        <label>
          Grade{' '}
          <select
            data-testid="rate-form-grade"
            value={grade}
            onChange={(e) => {
              setGrade(e.target.value);
              save.reset();
            }}
          >
            {GRADES.map((g) => (
              <option key={g} value={g}>
                {g}
              </option>
            ))}
          </select>
        </label>{' '}
        <label>
          Rate %{' '}
          <input
            data-testid="rate-form-pct"
            inputMode="decimal"
            placeholder="0.00"
            value={pct}
            onChange={(e) => {
              setPct(e.target.value);
              save.reset();
            }}
          />
        </label>{' '}
        <label>
          From{' '}
          <input
            type="date"
            data-testid="rate-form-from"
            value={from}
            onChange={(e) => {
              setFrom(e.target.value);
              save.reset();
            }}
          />
        </label>{' '}
        <label>
          To{' '}
          <input
            type="date"
            data-testid="rate-form-to"
            value={to}
            onChange={(e) => {
              setTo(e.target.value);
              save.reset();
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="rate-form-submit" disabled={save.isPending}>
          Set rate
        </button>
        {save.isError && (
          <p data-testid="rate-form-error" role="alert">
            {save.error.message}
          </p>
        )}
      </form>
      {rates.isError && (
        <p data-testid="rates-error" role="alert">
          {rates.error.message}
        </p>
      )}
      <table data-testid="rate-current-list">
        <thead>
          <tr>
            <th>Grade</th>
            <th>Rate % today</th>
          </tr>
        </thead>
        <tbody>
          {GRADES.map((g) => {
            const r = today === undefined ? undefined : rateOn(rates.data ?? [], g, today);
            return (
              <tr key={g}>
                <td>{g}</td>
                <td data-testid={`rate-current-${g}`}>{r ? showHundredths(toHundredths(r.rate)) : NA}</td>
              </tr>
            );
          })}
        </tbody>
      </table>
      <table data-testid="rate-list">
        <thead>
          <tr>
            <th>Grade</th>
            <th>From</th>
            <th>To</th>
            <th>Rate %</th>
            <th>Versions</th>
          </tr>
        </thead>
        <tbody>
          {(rates.data ?? []).filter((r) => (r.level ?? 0) === 0).map((r) => (
            <tr key={`${r.grade}-${r.from ?? 'start'}`} data-testid={`rate-row-${r.grade}-${r.from ?? 'start'}`}>
              <td>{r.grade}</td>
              <td data-testid="rate-from">{r.from ?? ''}</td>
              <td data-testid="rate-to">{r.to ?? ''}</td>
              <td data-testid="rate-pct">{showHundredths(toHundredths(r.rate))}</td>
              <td data-testid="rate-version">{r.version ?? 1}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <RatePreview sheetId={sheetId} />
    </section>
  );
}

// The rate preview: how many OPEN and PENDING claims of people on a grade (on the claim's date) would show a different due if a
// candidate rate applied to that grade on every date, and the sum of new due minus current due. A settled or bound claim keeps
// its figure, so it never changes. Nothing is saved. Changing the grade or the rate clears the result shown.
function RatePreview({ sheetId }: { sheetId: string }) {
  const floor = useFloorCents(sheetId);
  const late = useLate(sheetId);
  const terms = useRef({ floor, late });
  terms.current = { floor, late };
  const queryClient = useQueryClient();
  const [grade, setGrade] = useState<string>('A');
  const [pct, setPct] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<{ count: number; diff: bigint } | null>(null);

  const preview = async () => {
    setResult(null);
    setError(null);
    const rate = pct.trim();
    if (rate === '') return setError('Enter a rate');
    if (!RATE.test(rate) || Number(rate) > 100) return setError('A rate is a percent, 0.00 to 100.00, with up to two decimals');
    const base = `/api/sheets/${encodeURIComponent(sheetId)}`;
    // Re-read what the preview is worked out from, so it reflects the store now; the result is fixed as of this press.
    const [claims, people, current] = await Promise.all([
      queryClient.fetchQuery({ queryKey: claimsKey(sheetId), queryFn: () => getJson<Claim[]>(`${base}/claims`), staleTime: 0 }),
      queryClient.fetchQuery({ queryKey: peopleKey(sheetId), queryFn: () => getJson<Person[]>(`${base}/people`), staleTime: 0 }),
      queryClient.fetchQuery({ queryKey: ratesKey(sheetId), queryFn: () => getJson<Rate[]>(`${base}/rates`), staleTime: 0 }),
    ]);
    const { floor: floorOn, late: lateNow } = terms.current;
    const candidate: Rate[] = [...current.filter((r) => r.grade !== grade), { grade, rate, from: null, to: null }];
    let count = 0;
    let diff = 0n;
    for (const c of claims) {
      if (!isOpen(c)) continue;
      const g = gradeOn(people.find((p) => p.id === c.personId), c.date);
      if (g !== grade) continue;
      const was = claimDueCents(current, g, c, floorOn, lateNow);
      const now = claimDueCents(candidate, g, c, floorOn, lateNow);
      if (was === now) continue;
      count++;
      diff += (now ?? 0n) - (was ?? 0n);
    }
    setResult({ count, diff });
  };

  return (
    <form
      data-testid="rate-preview-form"
      onSubmit={(e) => {
        e.preventDefault();
        void preview();
      }}
    >
      <label>
        Preview grade{' '}
        <select
          data-testid="rate-preview-grade"
          value={grade}
          onChange={(e) => {
            setGrade(e.target.value);
            setError(null);
            setResult(null);
          }}
        >
          {GRADES.map((g) => (
            <option key={g} value={g}>
              {g}
            </option>
          ))}
        </select>
      </label>{' '}
      <label>
        Candidate rate %{' '}
        <input
          data-testid="rate-preview-pct"
          inputMode="decimal"
          placeholder="0.00"
          value={pct}
          onChange={(e) => {
            setPct(e.target.value);
            setError(null);
            setResult(null);
          }}
        />
      </label>{' '}
      <button type="submit" data-testid="rate-preview">
        Preview
      </button>
      {result !== null && (
        <span>
          {' '}Would change: <strong data-testid="rate-preview-count">{result.count}</strong>{' '}
          Difference: <strong data-testid="rate-preview-diff">{showHundredths(result.diff)}</strong>
        </span>
      )}
      {error !== null && (
        <p data-testid="rate-preview-error" role="alert">
          {error}
        </p>
      )}
    </form>
  );
}

function DueHead() {
  return <th>Due total</th>;
}

// A person's due-total: their counted claims' due figures added up, at most the sheet's cap; n/a when the grade has no rate today and
// no settled or stated claim keeps a figure.
function DueTotalCell({ sheetId, personId }: { sheetId: string; user: string; personId: number }) {
  const total = useDueTotals(sheetId).get(personId);
  return <td data-testid="person-due-total">{total == null ? NA : showHundredths(total)}</td>;
}

function SplitHeads() {
  return (
    <>
      <th>Settled total</th>
      <th>Open total</th>
    </>
  );
}

// A person's settled-total and open-total: their counted SETTLED claims' due, and their counted OPEN and PENDING claims' due, added up.
function SplitCells({ sheetId, personId }: { sheetId: string; user: string; personId: number }) {
  const split = useSplitTotals(sheetId).get(personId) ?? { settled: 0n, open: 0n };
  return (
    <>
      <td data-testid="person-settled-total">{showHundredths(split.settled)}</td>
      <td data-testid="person-open-total">{showHundredths(split.open)}</td>
    </>
  );
}

function ExcessHead() {
  return <th>Excess</th>;
}

// A person's excess: how far their figures add up to more than the sheet's cap; 0.00 when they do not.
function ExcessCell({ sheetId, personId }: { sheetId: string; user: string; personId: number }) {
  const excess = useExcesses(sheetId).get(personId) ?? 0n;
  return <td data-testid="person-excess">{showHundredths(excess)}</td>;
}

// A claim's due: fixed when it was settled; the bound figure while a statement binds it; otherwise its amount times the rate in force on the claim's
// date of its person's grade on that date; with its late charge on top while it is overdue, then at least the sheet's floor; n/a when no rate of the grade applies then.
function ClaimDue({ sheetId, personId, claim }: { sheetId: string; user: string; personId: number;
  claim: { amount: string; date: string; status: string; settledDue: string | null; stated: boolean; statedDue: string | null;
    dueBy: string; corrects?: string | null; deferred?: boolean } }) {
  const people = usePeople(sheetId);
  const rates = useRates(sheetId);
  const floor = useFloorCents(sheetId);
  const late = useLate(sheetId);
  const grade = gradeOn(people.data?.find((p) => p.id === personId), claim.date);
  const due = claimDueCents(rates.data ?? [], grade, claim, floor, late);
  return <span data-testid="claim-due">{due == null ? NA : showHundredths(due)}</span>;
}

/** The sheet's due-total: everyone's due-total added up, skipping those that are n/a, and leaving out the single highest
 *  and the single lowest when three or more people have one. */
export function useSheetDueTotal(sheetId: string): bigint {
  const totals = useDueTotals(sheetId);
  return trimmedSum([...totals.values()].filter((t): t is bigint => t != null));
}

// The sheet's due-total, as the rollup shows it.
function DueTotal({ sheetId }: { sheetId: string; user: string }) {
  const sum = useSheetDueTotal(sheetId);
  return (
    <section data-testid="due-rollup">
      <p>
        Due total: <strong data-testid="rollup-due-total">{showHundredths(sum)}</strong>
      </p>
    </section>
  );
}

// The sheet's settled-total and open-total: everyone's added up, untrimmed.
function SplitTotal({ sheetId }: { sheetId: string; user: string }) {
  const splits = [...useSplitTotals(sheetId).values()];
  const settled = splits.reduce((n, s) => n + s.settled, 0n);
  const open = splits.reduce((n, s) => n + s.open, 0n);
  return (
    <section data-testid="split-rollup">
      <p>
        Settled total: <strong data-testid="rollup-settled-total">{showHundredths(settled)}</strong>{' '}
        Open total: <strong data-testid="rollup-open-total">{showHundredths(open)}</strong>
      </p>
    </section>
  );
}

// For every tag ever used on the sheet, its due-total: the due of the counted claims carrying it, or whose person carries it,
// added up once each (n/a as 0); a claim with two tags counts in both.
function TagTotals({ sheetId }: { sheetId: string; user: string }) {
  const tags = useClaimTags(sheetId);
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const held = useHeld(sheetId);
  const floor = useFloorCents(sheetId);
  const late = useLate(sheetId);
  const sums = new Map<string, bigint>();
  for (const c of claims.data ?? []) {
    const person = people.data?.find((p) => p.id === c.personId);
    const carried = new Set([...c.tags, ...(person?.tags ?? [])]);
    if (carried.size === 0 || !counts(c, held)) continue;
    const due = claimDueCents(rates.data ?? [], gradeOn(person, c.date), c, floor, late) ?? 0n;
    for (const t of carried) sums.set(t, (sums.get(t) ?? 0n) + due);
  }
  return (
    <section data-testid="tag-rollup">
      <h2>Tags</h2>
      <table>
        <thead>
          <tr>
            <th>Tag</th>
            <th>Due total</th>
          </tr>
        </thead>
        <tbody>
          {(tags.data ?? []).map((t) => (
            <tr key={t}>
              <td>{t}</td>
              <td data-testid={`tag-${t}-due-total`}>{showHundredths(sums.get(t) ?? 0n)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

// For every calendar month (yyyy-mm) any claim on the sheet is dated in, its due-total: the due of the counted claims dated in
// that month added up (n/a as 0).
function MonthTotals({ sheetId }: { sheetId: string; user: string }) {
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const held = useHeld(sheetId);
  const floor = useFloorCents(sheetId);
  const late = useLate(sheetId);
  const sums = new Map<string, bigint>();
  for (const c of claims.data ?? []) {
    const month = c.date.slice(0, 7);
    if (!sums.has(month)) sums.set(month, 0n);
    if (!counts(c, held)) continue;
    const due = claimDueCents(rates.data ?? [], gradeOn(people.data?.find((p) => p.id === c.personId), c.date), c, floor, late) ?? 0n;
    sums.set(month, (sums.get(month) ?? 0n) + due);
  }
  return (
    <section data-testid="month-rollup">
      <h2>Months</h2>
      <table>
        <thead>
          <tr>
            <th>Month</th>
            <th>Due total</th>
          </tr>
        </thead>
        <tbody>
          {[...sums.keys()].sort().map((m) => (
            <tr key={m}>
              <td>{m}</td>
              <td data-testid={`month-${m}-due-total`}>{showHundredths(sums.get(m) ?? 0n)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contributions = [
  SheetBody.fill({ order: 5, Component: RatesPanel }),
  PersonHeads.fill({ order: 20, Component: DueHead }),
  PersonCells.fill({ order: 20, Component: DueTotalCell }),
  PersonHeads.fill({ order: 20.5, Component: SplitHeads }),
  PersonCells.fill({ order: 20.5, Component: SplitCells }),
  PersonHeads.fill({ order: 21, Component: ExcessHead }),
  PersonCells.fill({ order: 21, Component: ExcessCell }),
  ClaimCells.fill({ order: 10, Component: ClaimDue }),
  SheetBody.fill({ order: 25, Component: DueTotal }),
  SheetBody.fill({ order: 25.5, Component: SplitTotal }),
  SheetBody.fill({ order: 25.7, Component: TagTotals }),
  SheetBody.fill({ order: 25.8, Component: MonthTotals }),
];
