import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { GRADES } from '../people/usePeople';
import { useSheetToday } from '../sheet/useSheet';
import { PCT, rateOn, ratesKey, totalDue, useRates, type Rate } from './useRates';
import { claimDueCents, isOpen, useClaims } from '../claims/useClaims';
import { gradeOn, usePeople } from '../people/usePeople';
import { useFloor, useLate } from '../settings/useSettings';

/**
 * The rate in force on the sheet's today for each grade, every rate set (by grade, then from-date),
 * and one form to set a grade's rate, from a date or (left blank) from the beginning of time, through a date or (left blank) with no end.
 */
function RatesPanel({ user, sheet }: { user: string; sheet: string }) {
  const queryClient = useQueryClient();
  const { data: rates = [] } = useRates(sheet);
  const today = useSheetToday(sheet);
  const [grade, setGrade] = useState<string>('A');
  const [pct, setPct] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [invalid, setInvalid] = useState(false);

  const submitOnce = useSubmitOnce();
  const save = useMutation({
    mutationFn: (r: { grade: string; pct: string; from: string; to: string }) =>
      postJson<Rate>(`/api/sheets/${encodeURIComponent(sheet)}/rates`, { ...r, user }),
    onSuccess: async () => {
      setPct('');
      setFrom('');
      setTo('');
      await queryClient.invalidateQueries({ queryKey: ratesKey(sheet) });
    },
  });

  // An error goes as soon as its form is changed or submitted again.
  const clearError = () => {
    save.reset();
    setInvalid(false);
  };

  return (
    <section data-testid="rates-panel">
      <h2>Rates</h2>
      <form
        data-testid="rate-form"
        onSubmit={(e) => {
          e.preventDefault();
          if (!PCT.test(pct.trim())) {
            // Refused here, before anything is sent: the store and the audit file are untouched.
            save.reset();
            setInvalid(true);
            return;
          }
          setInvalid(false);
          submitOnce(() => save.mutateAsync({ grade, pct: pct.trim(), from: from.trim(), to: to.trim() }));
        }}
      >
        <label>
          Grade{' '}
          <select
            data-testid="rate-form-grade"
            value={grade}
            onChange={(e) => {
              setGrade(e.target.value);
              clearError();
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
            placeholder="0.00"
            value={pct}
            onChange={(e) => {
              setPct(e.target.value);
              clearError();
            }}
          />
        </label>{' '}
        <label>
          From{' '}
          <input
            data-testid="rate-form-from"
            placeholder="yyyy-mm-dd"
            value={from}
            onChange={(e) => {
              setFrom(e.target.value);
              clearError();
            }}
          />
        </label>{' '}
        <label>
          To{' '}
          <input
            data-testid="rate-form-to"
            placeholder="yyyy-mm-dd"
            value={to}
            onChange={(e) => {
              setTo(e.target.value);
              clearError();
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="rate-form-submit" disabled={save.isPending}>
          Set rate
        </button>
        {(invalid || save.isError) && (
          <span data-testid="rate-form-error" role="alert">
            {' '}
            {invalid
              ? 'The rate must be a percent, 0.00 or more, with up to two decimals.'
              : 'Could not set this rate.'}
          </span>
        )}
      </form>
      <RatePreview sheet={sheet} />
      <table data-testid="rate-current">
        <thead>
          <tr>
            <th>Grade</th>
            <th>Rate % today</th>
          </tr>
        </thead>
        <tbody>
          {GRADES.map((g) => (
            <tr key={g}>
              <td>{g}</td>
              <td data-testid={`rate-current-${g}`}>{rateOn(rates, g, today) ?? 'n/a'}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <table data-testid="rate-list">
        <thead>
          <tr>
            <th>Grade</th>
            <th>From</th>
            <th>To</th>
            <th>Rate %</th>
          </tr>
        </thead>
        <tbody>
          {rates.map((r) => (
            <tr key={`${r.grade}-${r.from ?? 'start'}`} data-testid={`rate-row-${r.grade}-${r.from ?? 'start'}`}>
              <td>{r.grade}</td>
              <td data-testid="rate-from">{r.from ?? 'start'}</td>
              <td data-testid="rate-to">{r.to ?? ''}</td>
              <td>
                <span data-testid="rate-pct">{r.pct}</span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

/**
 * A preview of a candidate rate for a grade, saving nothing: how many OPEN and PENDING claims of
 * people on that grade (the grade in force on the claim's date) would show a different due if the
 * rate applied to the grade on every date, and the differences (new due minus current) added up.
 * A claim bound by a statement keeps its figure, so it does not change; a settled claim is not counted.
 */
function RatePreview({ sheet }: { sheet: string }) {
  const claimsQuery = useClaims(sheet);
  const peopleQuery = usePeople(sheet);
  const ratesQuery = useRates(sheet);
  const floor = useFloor(sheet);
  const late = useLate(sheet);
  const [grade, setGrade] = useState<string>('A');
  const [pct, setPct] = useState('');
  const [invalid, setInvalid] = useState(false);
  const [running, setRunning] = useState(false);
  const [result, setResult] = useState<{ count: number; diff: string } | null>(null);

  const preview = async () => {
    setResult(null);
    if (!PCT.test(pct.trim())) {
      setInvalid(true);
      return;
    }
    setInvalid(false);
    setRunning(true);
    try {
      // Re-read, so the preview goes by what the store holds now.
      const [claims, people, rates] = await Promise.all([claimsQuery.refetch(), peopleQuery.refetch(), ratesQuery.refetch()]);
      const byId = new Map((people.data ?? []).map((p) => [p.id, p]));
      const candidate = pct.trim();
      let count = 0;
      let diff = 0n;
      for (const c of claims.data ?? []) {
        const person = byId.get(c.personId);
        if (!isOpen(c) || person === undefined || gradeOn(person, c.date) !== grade) continue;
        const current = claimDueCents(c, floor, rateOn(rates.data ?? [], grade, c.date), late);
        const next = claimDueCents(c, floor, candidate, late);
        if (current === next) continue;
        count++;
        diff += (next ?? 0n) - (current ?? 0n);
      }
      setResult({ count, diff: totalDue([diff]) });
    } finally {
      setRunning(false);
    }
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
            setInvalid(false);
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
          placeholder="0.00"
          value={pct}
          onChange={(e) => {
            setPct(e.target.value);
            setInvalid(false);
          }}
        />
      </label>{' '}
      <button type="submit" data-testid="rate-preview" disabled={running}>
        Preview
      </button>
      {result !== null && (
        <span>
          {' '}
          Claims changing: <span data-testid="rate-preview-count">{result.count}</span>, difference:{' '}
          <span data-testid="rate-preview-diff">{result.diff}</span>
        </span>
      )}
      {invalid && (
        <span data-testid="rate-preview-error" role="alert">
          {' '}
          The rate must be a percent, 0.00 or more, with up to two decimals.
        </span>
      )}
    </form>
  );
}

export const contribution = SheetPanels.fill({ order: 5, Component: RatesPanel });
