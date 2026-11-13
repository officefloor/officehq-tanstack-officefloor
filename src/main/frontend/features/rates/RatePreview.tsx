import { useState } from 'react';
import type { FormEvent } from 'react';
import { useClaims } from '../claims/claims';
import { GRADES, usePeople } from '../people/people';
import { useDueRules } from '../settings/settings';
import { PERCENT, ratePreview, useRates } from './rates';

// A grade and a candidate rate, previewed: how many OPEN and PENDING claims on that grade would show a different
// due were that the grade's rate on every date, and by how much in all. Nothing is saved.
export function RatePreview() {
  const rates = useRates().data ?? [];
  const people = usePeople().data ?? [];
  const claims = useClaims().data ?? [];
  const rules = useDueRules();
  const [grade, setGrade] = useState<string>('A');
  const [pct, setPct] = useState('');
  const [result, setResult] = useState<{ count: number; diff: string } | null>(null);
  const [error, setError] = useState('');

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setResult(null);
    setError('');
    if (!PERCENT.test(pct.trim())) {
      setError('Type a percent of 0.00 or more, with at most two decimals.');
      return;
    }
    const gradesOf = new Map(people.map((p) => [p.id, p.grades]));
    const rate = Number(pct.trim()).toFixed(2);
    // Shown on the next tick, so two presses with the same result are told apart.
    setTimeout(() => setResult(ratePreview(claims, (id) => gradesOf.get(id) ?? [], rates, grade, rate, rules)), 0);
  };

  const change = (apply: () => void) => {
    apply();
    setResult(null);
    setError('');
  };

  return (
    <form onSubmit={submit} data-testid="rate-preview-form">
      <select data-testid="rate-preview-grade" value={grade} onChange={(e) => change(() => setGrade(e.target.value))}>
        {GRADES.map((g) => (
          <option key={g} value={g}>
            {g}
          </option>
        ))}
      </select>
      <input
        data-testid="rate-preview-pct"
        placeholder="Candidate rate %"
        value={pct}
        onChange={(e) => change(() => setPct(e.target.value))}
      />
      <button type="submit" data-testid="rate-preview">
        Preview
      </button>
      {result && (
        <span>
          {' '}
          Claims changed: <strong data-testid="rate-preview-count">{result.count}</strong>, difference:{' '}
          <strong data-testid="rate-preview-diff">{result.diff}</strong>
        </span>
      )}
      {error && <span data-testid="rate-preview-error">{error}</span>}
    </form>
  );
}
