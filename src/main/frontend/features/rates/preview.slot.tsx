import { useState } from 'react';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet } from '../../session/session';
import { usePeople } from '../people/people';
import { useSheetToday } from '../sheet/sheet';
import { useClaims } from '../claims/claims';
import { floorOf, useSettings } from '../settings/settings';
import { lateOf } from '../overdue/overdue';
import { ratePreview, useRates } from './rates';
import { useFigure } from '../locale/locale';
import { useGradeCodes } from '../grades/grades';

/** 0.00 or more, with up to two decimals — as a rate is set. */
const PCT = /^\d+(\.\d{1,2})?$/;

// The rates panel's preview: how many OPEN and PENDING claims of people on a grade (on the claim's date)
// would show a different due were a candidate percent the grade's rate on every date (a retired or held
// person's claims too), and the sum of the differences. Worked out on the screen; nothing is saved.
function RatePreview() {
  const fig = useFigure();
  const [sheet] = useCurrentSheet();
  const grades = useGradeCodes(sheet);
  const people = usePeople(sheet);
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const today = useSheetToday(sheet);
  const settings = useSettings(sheet).data;
  const [grade, setGrade] = useState<string>('A');
  const [pct, setPct] = useState('');
  const [result, setResult] = useState<{ count: number; diff: string } | null>(null);
  const [error, setError] = useState(false);

  const preview = () => {
    setResult(null);
    setError(false);
    const candidate = pct.trim();
    if (!PCT.test(candidate)) {
      setError(true);
      return;
    }
    const all = people.data ?? [];
    const shown = all.map((p) => ({
      grades: p.grades,
      claims: (claims.data ?? []).filter((c) => c.personId === p.id),
      late: lateOf(settings, today, [p]),
    }));
    // Shown on the next tick, so the old count is gone while the preview runs.
    setTimeout(() => setResult(ratePreview(shown, rates.data, grade, candidate, floorOf(settings))), 0);
  };

  return (
    <section data-testid="rate-preview-panel">
      <h3>Rate preview</h3>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          preview();
        }}
      >
        <select
          data-testid="rate-preview-grade"
          value={grade}
          onChange={(e) => {
            setGrade(e.target.value);
            setError(false);
            setResult(null);
          }}
        >
          {grades.map((g) => (
            <option key={g} value={g}>
              {g}
            </option>
          ))}
        </select>
        <input
          data-testid="rate-preview-pct"
          placeholder="Candidate rate %"
          inputMode="decimal"
          value={pct}
          onChange={(e) => {
            setPct(e.target.value);
            setError(false);
            setResult(null);
          }}
        />
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
        {error && (
          <span data-testid="rate-preview-error" role="alert">
            {' '}
            The rate must be a percent of 0.00 or more with up to two decimals.
          </span>
        )}
      </form>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 40.5, Component: RatePreview });
