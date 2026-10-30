import { useState } from 'react';
import { isOpen, useClaims } from '../claims/useClaims';
import { useGrace } from '../claims/useGrace';
import { GRADES, gradeOn, usePeople } from '../people/usePeople';
import { useFloor, useLatePct, usePolicy } from '../settings/useSettings';
import { useSheetToday } from '../sheet/useSheetToday';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { claimDue, formatCents } from './due';
import { rateOn, useRates } from './useRates';

const PCT = /^\d+(\.\d{1,2})?$/;

// A what-if on a grade's rate: how many OPEN and PENDING claims of people on the grade (the one in force
// on the claim's date) would show a different due if the candidate rate applied to it on every date, and
// the sum of new due minus current due. Worked here with the same figures the claim rows show, so a
// claim bound by a statement (or settled, which is left out) never moves. Nothing is saved.
export function RatePreview() {
  const [sheet] = useSearchParam('sheet', asString);
  const people = usePeople(sheet);
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const today = useSheetToday(sheet);
  const floor = useFloor(sheet);
  const grace = useGrace(sheet);
  const late = { pct: useLatePct(sheet), today, grace };
  const policy = usePolicy(sheet);
  const [grade, setGrade] = useState<string>('A');
  const [pct, setPct] = useState('');
  const [result, setResult] = useState<{ count: number; diff: string } | null>(null);
  const [error, setError] = useState(false);
  const changed = () => {
    setResult(null);
    setError(false);
  };
  const preview = () => {
    const candidate = pct.trim();
    if (!PCT.test(candidate)) {
      setError(true);
      return;
    }
    const byId = new Map(people.map((p) => [p.id, p]));
    let count = 0;
    let diff = 0n;
    for (const c of claims) {
      const person = byId.get(c.personId);
      if (person === undefined || !isOpen(c) || gradeOn(person, c.date) !== grade) continue;
      const current = claimDue(c, rateOn(rates, grade, c.date), floor, late, policy);
      const next = claimDue(c, candidate, floor, late, policy);
      if (current === next) continue;
      count++;
      diff += (next ?? 0n) - (current ?? 0n);
    }
    setResult({ count, diff: formatCents(diff) });
  };
  return (
    <form
      data-testid="rate-preview-form"
      onSubmit={(e) => {
        e.preventDefault();
        changed();
        // The old count is gone while the preview is worked, so two presses with the same result differ.
        setTimeout(preview, 0);
      }}
    >
      <select
        data-testid="rate-preview-grade"
        value={grade}
        onChange={(e) => {
          setGrade(e.target.value);
          changed();
        }}
      >
        {GRADES.map((g) => (
          <option key={g} value={g}>{g}</option>
        ))}
      </select>{' '}
      <input
        data-testid="rate-preview-pct"
        value={pct}
        placeholder="Candidate rate %"
        onChange={(e) => {
          setPct(e.target.value);
          changed();
        }}
      />{' '}
      <button type="submit" data-testid="rate-preview">Preview</button>
      {result !== null && (
        <span>
          {' '}Would change <span data-testid="rate-preview-count">{result.count}</span> claim(s) by{' '}
          <span data-testid="rate-preview-diff">{result.diff}</span>
        </span>
      )}
      {error && <span data-testid="rate-preview-error" role="alert"> Could not preview: the rate must be a percent of 0.00 or more with at most two decimals.</span>}
    </form>
  );
}
