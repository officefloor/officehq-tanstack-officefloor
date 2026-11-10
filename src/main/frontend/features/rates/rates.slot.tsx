import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSingleFlight } from '../../query/useSingleFlight';
import { PersonCells, PersonColumnHead } from '../../slots/defs/personRow';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useGrades } from '../people/usePeople';
import { useSheetToday } from '../today/useSheetToday';
import { useDueTotals, useExcess, useSettledOpenTotals } from './useDueTotals';
import { useRatePreview } from './useRatePreview';
import { rateFor, ratesKey, trimmedSum, useRates, type Rate } from './useRates';
import { useFigure } from '../locale/useLocale';
import { Col } from '../columns/useColumns';

/** The sheet's rates for each grade: a form that sets one from an optional from-date (a new version of the
 *  grade's rate from that date, the one in force; blank is from the beginning of time) through an optional to-date
 *  (blank is no end), the rates set, and the rate in
 *  force on the sheet's today for every grade ('n/a' when none is). A steward withdraws a rate set from its row (its newest
 *  version; the one before it applies again). */
function Rates({ sheetId }: { sheetId: string }) {
  const grades = useGrades(sheetId);
  const fig = useFigure();
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
  // A steward withdraws a rate's newest version (the one before it applies again); a refusal shows in the
  // form's error.
  const withdraw = useMutation({
    mutationFn: (r: { grade: string; from: string }) =>
      postJson<Rate>(`/api/sheets/${encodeURIComponent(sheetId)}/rate-withdraw`, r),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ratesKey(sheetId) });
    },
  });
  const resetAll = () => {
    save.reset();
    withdraw.reset();
  };

  return (
    <section data-testid="rates">
      <form
        data-testid="rate-form"
        onSubmit={(e) => {
          e.preventDefault();
          submitOnce(() => {
            resetAll();
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
              resetAll();
            }}
          >
            {grades.map((g) => (
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
              resetAll();
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
              resetAll();
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
              resetAll();
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="rate-form-submit" disabled={save.isPending}>
          Set rate
        </button>
        {(save.isError || withdraw.isError) && (
          <p data-testid="rate-form-error">{save.error?.message ?? withdraw.error?.message}</p>
        )}
      </form>
      <table data-testid="rates-list">
        <thead>
          <tr>
            <th>Grade</th>
            <th>Rate %</th>
            <th>From</th>
            <th>To</th>
            <th>Versions</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {(rates.data ?? []).filter((r) => r.level === 0 && !r.superseded).map((r) => (
            <tr key={`${r.grade}-${r.from ?? 'start'}`} data-testid={`rate-row-${r.grade}-${r.from ?? 'start'}`}>
              <td data-testid="rate-grade">{r.grade}</td>
              <td data-testid="rate-pct">{fig(r.pct)}</td>
              <td data-testid="rate-from">{r.from ?? ''}</td>
              <td data-testid="rate-to">{r.to ?? ''}</td>
              <td data-testid="rate-version">{r.version ?? 1}</td>
              <td>
                <button
                  type="button"
                  data-testid="rate-withdraw"
                  disabled={withdraw.isPending}
                  onClick={() =>
                    submitOnce(() => {
                      resetAll();
                      return withdraw.mutateAsync({ grade: r.grade, from: r.from ?? '' });
                    })
                  }
                >
                  Withdraw
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <p>
        Current rates:{' '}
        {grades.map((g) => (
          <span key={g}>
            {g} <strong data-testid={`rate-current-${g}`}>{fig(rateFor(rates.data, g, today.data?.today) ?? 'n/a')}</strong>{' '}
          </span>
        ))}
      </p>
      <RateCalendar sheetId={sheetId} />
      <ImportRates sheetId={sheetId} />
      <RatePreview sheetId={sheetId} />
      {rates.isError && <p data-testid="rates-error">{rates.error.message}</p>}
    </section>
  );
}

/** The rate calendar: for every grade and each month of the sheet's today's calendar year, the rate in force
 *  on the first day of that month ('n/a' when none is). */
function RateCalendar({ sheetId }: { sheetId: string }) {
  const grades = useGrades(sheetId);
  const fig = useFigure();
  const rates = useRates(sheetId);
  const year = useSheetToday(sheetId).data?.today?.slice(0, 4);
  if (year === undefined) return null;
  const months = Array.from({ length: 12 }, (_, i) => `${year}-${String(i + 1).padStart(2, '0')}`);
  return (
    <table data-testid="rate-calendar">
      <thead>
        <tr>
          <th>Grade</th>
          {months.map((m) => (
            <th key={m}>{m}</th>
          ))}
        </tr>
      </thead>
      <tbody>
        {grades.map((g) => (
          <tr key={g}>
            <td>{g}</td>
            {months.map((m) => (
              <td key={m} data-testid={`rate-month-${g}-${m}`}>{fig(rateFor(rates.data, g, `${m}-01`) ?? 'n/a')}</td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  );
}

/** A rate import box: lines of grade,rate,from,to (the from and to may be absent or blank), each set in
 *  order as the rate form sets it. The server checks every line first: one refused line refuses the import
 *  and sets nothing. Shows how many were set (gone while it runs). */
function ImportRates({ sheetId }: { sheetId: string }) {
  const [text, setText] = useState('');
  const queryClient = useQueryClient();
  const submitOnce = useSingleFlight();
  const importRates = useMutation({
    mutationFn: (t: string) =>
      postJson<{ set: number }>(`/api/sheets/${encodeURIComponent(sheetId)}/rate-import`, { text: t }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ratesKey(sheetId) });
    },
  });
  return (
    <form
      data-testid="import-rates-form"
      onSubmit={(e) => {
        e.preventDefault();
        submitOnce(() => {
          importRates.reset();
          return importRates.mutateAsync(text);
        });
      }}
    >
      <label>
        Import rates (grade,rate,from,to per line){' '}
        <textarea
          data-testid="import-rates"
          rows={3}
          value={text}
          onChange={(e) => {
            setText(e.target.value);
            importRates.reset();
          }}
        />
      </label>{' '}
      <button type="submit" data-testid="import-rates-submit" disabled={importRates.isPending}>
        Import rates
      </button>
      {importRates.isSuccess && (
        <>
          {' '}
          Set <strong data-testid="import-rates-set">{importRates.data.set}</strong>
        </>
      )}
      {importRates.isError && <p data-testid="import-rates-error">{importRates.error.message}</p>}
    </form>
  );
}

/** A preview of a candidate rate for a grade (see `useRatePreview`): how many claims would show a different
 *  due and the sum of the differences. Nothing is saved. The result is cleared when the form changes. */
function RatePreview({ sheetId }: { sheetId: string }) {
  const grades = useGrades(sheetId);
  const fig = useFigure();
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
          {grades.map((g) => (
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
          <strong data-testid="rate-preview-diff">{fig(result.diff)}</strong>
        </span>
      )}
      {error !== null && <p data-testid="rate-preview-error">{error}</p>}
    </form>
  );
}

function PersonDueTotal({ sheetId, personId }: { sheetId: string; personId: number }) {
  const fig = useFigure();
  const totals = useDueTotals(sheetId);
  return <Col id="due-total"><td data-testid="person-due-total">{fig(totals.get(personId) ?? '')}</td></Col>;
}

/** A person's excess: how far the sum of their figures is above the sheet's cap. */
function PersonExcess({ sheetId, personId }: { sheetId: string; personId: number }) {
  const fig = useFigure();
  const excess = useExcess(sheetId);
  return <Col id="excess"><td data-testid="person-excess">{fig(excess.get(personId) ?? '')}</td></Col>;
}

/** A person's settled-total and open-total: the parts of their due-total (before the cap) from their
 *  SETTLED claims and from their open (OPEN or PENDING) claims. */
function PersonSettledOpen({ sheetId, personId }: { sheetId: string; personId: number }) {
  const fig = useFigure();
  const split = useSettledOpenTotals(sheetId).get(personId);
  return (
    <>
      <td data-testid="person-settled-total">{fig(split?.settled ?? '')}</td>
      <td data-testid="person-open-total">{fig(split?.open ?? '')}</td>
    </>
  );
}

/** The sheet's settled-total and open-total: everyone's figure added up, leaving out the highest and the
 *  lowest when three or more people have one. */
function SettledOpenTotals({ sheetId }: { sheetId: string }) {
  const fig = useFigure();
  const split = [...useSettledOpenTotals(sheetId).values()];
  return (
    <p>
      Settled total: <strong data-testid="rollup-settled-total">{fig(trimmedSum(split.map((s) => s.settled)))}</strong>,
      open total <strong data-testid="rollup-open-total">{fig(trimmedSum(split.map((s) => s.open)))}</strong>
    </p>
  );
}

/** The sheet's due-total: everyone's due-total added up, skipping 'n/a',
 *  leaving out the highest and the lowest when three or more people have one. */
function DueTotal({ sheetId }: { sheetId: string }) {
  const fig = useFigure();
  const totals = useDueTotals(sheetId);
  return (
    <p>
      Due total: <strong data-testid="rollup-due-total">{fig(trimmedSum([...totals.values()]))}</strong>
    </p>
  );
}

export const contributions = [
  SheetBody.fill({ order: 5, Component: ({ sheetId }) => <Rates sheetId={sheetId} /> }),
  PersonColumnHead.fill({ order: 20, Component: () => <Col id="due-total"><th>Due</th></Col> }),
  PersonCells.fill({ order: 20, Component: PersonDueTotal }),
  PersonColumnHead.fill({ order: 21, Component: () => <Col id="excess"><th>Excess</th></Col> }),
  PersonCells.fill({ order: 21, Component: PersonExcess }),
  PersonColumnHead.fill({ order: 21.5, Component: () => <><th>Settled</th><th>Open</th></> }),
  PersonCells.fill({ order: 21.5, Component: PersonSettledOpen }),
  SheetBody.fill({ order: 30, Component: ({ sheetId }) => <DueTotal sheetId={sheetId} /> }),
  SheetBody.fill({ order: 31, Component: ({ sheetId }) => <SettledOpenTotals sheetId={sheetId} /> }),
];
