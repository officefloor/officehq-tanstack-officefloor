import { useState, type ChangeEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSingleFlight } from '../../query/useSingleFlight';
import { PERSON_ROW_SPAN, PersonDetail } from '../../slots/defs/personRow';
import { SheetBody } from '../../slots/defs/sheetBody';
import { claimsKey } from '../claims/useClaims';
import { peopleKey } from '../people/usePeople';
import { useSheetToday } from '../today/useSheetToday';
import { plansKey, usePlans, type Plan } from './usePlans';
import { useFigure } from '../locale/useLocale';

const DAY_MS = 86_400_000;

const isoDay = (ms: number) => new Date(ms).toISOString().slice(0, 10);

/** A plan's first five dates after today and on or before its end, in order: the start then every N days, or
 *  (monthly) that day in every month from the start's month, falling on the month's last day when beyond it,
 *  those before the start left out — the same dates the generate makes. */
export function nextPlanDates(plan: Plan, today: string): string[] {
  const out: string[] = [];
  const start = Date.parse(`${plan.start}T00:00:00Z`);
  const end = Date.parse(`${plan.end}T00:00:00Z`);
  const after = Date.parse(`${today}T00:00:00Z`);
  if (Number.isNaN(start) || Number.isNaN(end) || Number.isNaN(after)) return out;
  if (plan.day == null) {
    const step = (plan.every ?? 0) * DAY_MS;
    if (step <= 0) return out;
    let on = start;
    if (on <= after) on += Math.floor((after - on) / step + 1) * step;
    for (; on <= end && out.length < 5; on += step) out.push(isoDay(on));
    return out;
  }
  const first = new Date(start);
  for (let m = first.getUTCFullYear() * 12 + first.getUTCMonth(); out.length < 5; m++) {
    const y = Math.floor(m / 12);
    const mo = m % 12;
    if (Date.UTC(y, mo, 1) > end) break;
    const last = new Date(Date.UTC(y, mo + 1, 0)).getUTCDate();
    const on = Date.UTC(y, mo, Math.min(plan.day, last));
    if (on >= start && on > after && on <= end) out.push(isoDay(on));
  }
  return out;
}

/** A person's plans, each with how many claims it has made so far and its next five dates after the sheet's today, and a form that adds one: an amount, a
 *  start date, every N days (a whole number, at least 1) or a day of the month (1 to 31), and an end date not before the start. A refused plan
 *  shows an error on the row. */
function PersonPlans({ sheetId, personId }: { sheetId: string; personId: number }) {
  const fig = useFigure();
  const plans = usePlans(sheetId);
  const today = useSheetToday(sheetId).data?.today;
  const queryClient = useQueryClient();
  const submitOnce = useSingleFlight();
  const [amount, setAmount] = useState('');
  const [start, setStart] = useState('');
  const [every, setEvery] = useState('');
  const [day, setDay] = useState('');
  const [end, setEnd] = useState('');

  const add = useMutation({
    mutationFn: (f: { amount: string; start: string; every: string; day: string; end: string }) =>
      postJson<{ plans: Plan[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/plans`, f),
    onSuccess: (data) => {
      setAmount('');
      setStart('');
      setEvery('');
      setDay('');
      setEnd('');
      queryClient.setQueryData(plansKey(sheetId), data.plans);
    },
  });

  const field = (set: (v: string) => void) => (e: ChangeEvent<HTMLInputElement>) => {
    set(e.target.value);
    add.reset();
  };

  const mine = (plans.data ?? []).filter((p) => p.personId === personId);

  return (
    <tr>
      <td colSpan={PERSON_ROW_SPAN}>
        {mine.length > 0 && (
          <table>
            <thead>
              <tr>
                <th>Plan amount</th>
                <th>Start</th>
                <th>Every (days)</th>
                <th>Day of month</th>
                <th>End</th>
                <th>Generated</th>
                <th>Next dates</th>
              </tr>
            </thead>
            <tbody>
              {mine.map((p) => (
                <tr key={p.id} data-testid={`plan-row-${p.id}`}>
                  <td data-testid="plan-amount-shown">{fig(p.amount)}</td>
                  <td data-testid="plan-start">{p.start}</td>
                  <td data-testid="plan-every">{p.every ?? ''}</td>
                  <td data-testid="plan-day">{p.day ?? ''}</td>
                  <td data-testid="plan-end">{p.end}</td>
                  <td data-testid="plan-generated">{p.generated}</td>
                  <td data-testid="plan-next-dates">{today ? nextPlanDates(p, today).join(', ') : ''}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        <form
          data-testid="plan-form"
          onSubmit={(e) => {
            e.preventDefault();
            submitOnce(() => {
              add.reset();
              return add.mutateAsync({ amount: amount.trim(), start: start.trim(), every: every.trim(), day: day.trim(), end: end.trim() });
            });
          }}
        >
          Plan:{' '}
          <input type="text" placeholder="amount" data-testid="plan-form-amount" value={amount} onChange={field(setAmount)} />{' '}
          <input type="text" placeholder="start yyyy-mm-dd" data-testid="plan-form-start" value={start} onChange={field(setStart)} />{' '}
          <input type="text" placeholder="every N days" data-testid="plan-form-every" value={every} onChange={field(setEvery)} />{' '}
          <input type="text" placeholder="or day of month 1-31" data-testid="plan-form-day" value={day} onChange={field(setDay)} />{' '}
          <input type="text" placeholder="end yyyy-mm-dd" data-testid="plan-form-end" value={end} onChange={field(setEnd)} />{' '}
          <button type="submit" data-testid="plan-form-submit" disabled={add.isPending}>
            Add plan
          </button>
          {add.isError && <span data-testid="plan-error">{add.error.message}</span>}
        </form>
      </td>
    </tr>
  );
}

/** Generate the plans' claims: a DRAFT claim for every plan date up to its end and the sheet's today that has
 *  none yet. Shows how many were made (gone while it runs). */
function GeneratePlans({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const submitOnce = useSingleFlight();
  const generate = useMutation({
    mutationFn: () => postJson<{ made: number }>(`/api/sheets/${encodeURIComponent(sheetId)}/plan-generate`, {}),
    // The new claims are re-read before the count shows, so a count on screen means its rows are there too.
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: plansKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
      ]);
    },
  });
  return (
    <section>
      <button
        type="button"
        data-testid="plan-generate"
        disabled={generate.isPending}
        onClick={() =>
          submitOnce(() => {
            generate.reset();
            return generate.mutateAsync();
          })
        }
      >
        Generate plan claims
      </button>
      {generate.isSuccess && (
        <>
          {' '}
          Made <strong data-testid="plan-generated-count">{generate.data.made}</strong>
        </>
      )}
      {generate.isError && <p data-testid="plan-generate-error">{generate.error.message}</p>}
    </section>
  );
}

export const contributions = [
  PersonDetail.fill({ order: 20, Component: PersonPlans }),
  SheetBody.fill({ order: 18, Component: ({ sheetId }) => <GeneratePlans sheetId={sheetId} /> }),
];
