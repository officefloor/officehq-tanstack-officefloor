import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSingleFlight } from '../../query/useSingleFlight';
import { PersonCells, PersonColumnHead } from '../../slots/defs/personRow';
import { SheetBody } from '../../slots/defs/sheetBody';
import { GRADES } from '../people/usePeople';
import { useSheetToday } from '../today/useSheetToday';
import { useDueTotals, useExcess, useSettledOpenTotals } from './useDueTotals';
import { useRatePreview } from './useRatePreview';
import { rateFor, ratesKey, sumDues, trimmedSum, useRates, type Rate } from './useRates';

/** The sheet's rates for each grade: a form that sets one from an optional from-date (replacing the
 *  grade's rate from that date; blank is from the beginning of time) through an optional to-date
 *  (blank is no end), the rates set, and the rate in
 *  force on the sheet's today for every grade ('n/a' when none is). */
function Rates({ sheetId }: { sheetId: string }) {
  const rates = useRates(sheetId);
  const today = useSheetToday(sheetId);
  const queryClient = useQueryClient();
  const [grade, setGrade] = useState<string>('A');
  const [pct, setPct] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');

  const submitOnce = useSingleFlight();
  const save = useMutation({
    mutationFn: (f: { grade: string; pct: string; from: string; to: string }) =>
      postJson<Rate>(`/api/sheets/${encodeURIComponent(sheetId)}/rates`, f),
    onSuccess: async () => {
      setPct('');
      setFrom('');
      setTo('');
      await queryClient.invalidateQueries({ queryKey: ratesKey(sheetId) });
    },
  });

  return (
    <section data-testid="rates">
      <form
        data-testid="rate-form"
        onSubmit={(e) => {
          e.preventDefault();
          submitOnce(() => {
            save.reset();
            return save.mutateAsync({ grade, pct, from, to });
          });
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
            type="text"
            placeholder="yyyy-mm-dd"
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
            type="text"
            placeholder="yyyy-mm-dd"
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
        {save.isError && <p data-testid="rate-form-error">{save.error.message}</p>}
      </form>
      <table data-testid="rates-list">
        <thead>
          <tr>
            <th>Grade</th>
            <th>Rate %</th>
            <th>From</th>
            <th>To</th>
          </tr>
        </thead>
        <tbody>
          {(rates.data ?? []).filter((r) => r.level === 0).map((r) => (
            <tr key={`${r.grade}-${r.from ?? 'start'}`} data-testid={`rate-row-${r.grade}-${r.from ?? 'start'}`}>
              <td data-testid="rate-grade">{r.grade}</td>
              <td data-testid="rate-pct">{r.pct}</td>
              <td data-testid="rate-from">{r.from ?? ''}</td>
              <td data-testid="rate-to">{r.to ?? ''}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <p>
        Current rates:{' '}
        {GRADES.map((g) => (
          <span key={g}>
            {g} <strong data-testid={`rate-current-${g}`}>{rateFor(rates.data, g, today.data?.today) ?? 'n/a'}</strong>{' '}
          </span>
        ))}
      </p>
      <RatePreview sheetId={sheetId} />
      {rates.isError && <p data-testid="rates-error">{rates.error.message}</p>}
    </section>
  );
}

/** A preview of a candidate rate for a grade (see `useRatePreview`): how many claims would show a different
 *  due and the sum of the differences. Nothing is saved. The result is cleared when the form changes. */
function RatePreview({ sheetId }: { sheetId: string }) {
  const preview = useRatePreview(sheetId);
  const [grade, setGrade] = useState<string>('A');
  const [pct, setPct] = useState('');
  const [result, setResult] = useState<{ count: number; diff: string } | null>(null);
  const [error, setError] = useState<string | null>(null);
  const clear = () => {
    setResult(null);
    setError(null);
  };

  return (
    <form
      data-testid="rate-preview-form"
      onSubmit={(e) => {
        e.preventDefault();
        clear();
        try {
          const r = preview(grade, pct);
          setTimeout(() => setResult(r), 0);
        } catch (err) {
          setError((err as Error).message);
        }
      }}
    >
      <label>
        Preview grade{' '}
        <select
          data-testid="rate-preview-grade"
          value={grade}
          onChange={(e) => {
            setGrade(e.target.value);
            clear();
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
          data-testid="rate-preview-pct"
          inputMode="decimal"
          value={pct}
          onChange={(e) => {
            setPct(e.target.value);
            clear();
          }}
        />
      </label>{' '}
      <button type="submit" data-testid="rate-preview">
        Preview
      </button>
      {result !== null && (
        <span>
          {' '}
          Would change <strong data-testid="rate-preview-count">{result.count}</strong> claims by{' '}
          <strong data-testid="rate-preview-diff">{result.diff}</strong>
        </span>
      )}
      {error !== null && <p data-testid="rate-preview-error">{error}</p>}
    </form>
  );
}

function PersonDueTotal({ sheetId, personId }: { sheetId: string; personId: number }) {
  const totals = useDueTotals(sheetId);
  return <td data-testid="person-due-total">{totals.get(personId) ?? ''}</td>;
}

/** A person's excess: how far the sum of their figures is above the sheet's cap. */
function PersonExcess({ sheetId, personId }: { sheetId: string; personId: number }) {
  const excess = useExcess(sheetId);
  return <td data-testid="person-excess">{excess.get(personId) ?? ''}</td>;
}

/** A person's settled-total and open-total: the parts of their due-total (before the cap) from their
 *  SETTLED claims and from their open (OPEN or PENDING) claims. */
function PersonSettledOpen({ sheetId, personId }: { sheetId: string; personId: number }) {
  const split = useSettledOpenTotals(sheetId).get(personId);
  return (
    <>
      <td data-testid="person-settled-total">{split?.settled ?? ''}</td>
      <td data-testid="person-open-total">{split?.open ?? ''}</td>
    </>
  );
}

/** The sheet's settled-total and open-total: everyone's figure simply added up (not trimmed). */
function SettledOpenTotals({ sheetId }: { sheetId: string }) {
  const split = [...useSettledOpenTotals(sheetId).values()];
  return (
    <p>
      Settled total: <strong data-testid="rollup-settled-total">{sumDues(split.map((s) => s.settled))}</strong>,
      open total <strong data-testid="rollup-open-total">{sumDues(split.map((s) => s.open))}</strong>
    </p>
  );
}

/** The sheet's due-total: everyone's due-total added up, skipping 'n/a',
 *  leaving out the highest and the lowest when three or more people have one. */
function DueTotal({ sheetId }: { sheetId: string }) {
  const totals = useDueTotals(sheetId);
  return (
    <p>
      Due total: <strong data-testid="rollup-due-total">{trimmedSum([...totals.values()])}</strong>
    </p>
  );
}

export const contributions = [
  SheetBody.fill({ order: 5, Component: ({ sheetId }) => <Rates sheetId={sheetId} /> }),
  PersonColumnHead.fill({ order: 20, Component: () => <th>Due</th> }),
  PersonCells.fill({ order: 20, Component: PersonDueTotal }),
  PersonColumnHead.fill({ order: 21, Component: () => <th>Excess</th> }),
  PersonCells.fill({ order: 21, Component: PersonExcess }),
  PersonColumnHead.fill({ order: 21.5, Component: () => <><th>Settled</th><th>Open</th></> }),
  PersonCells.fill({ order: 21.5, Component: PersonSettledOpen }),
  SheetBody.fill({ order: 30, Component: ({ sheetId }) => <DueTotal sheetId={sheetId} /> }),
  SheetBody.fill({ order: 31, Component: ({ sheetId }) => <SettledOpenTotals sheetId={sheetId} /> }),
];
