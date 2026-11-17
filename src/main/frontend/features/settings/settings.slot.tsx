import { useRef, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { SheetPage } from '../../slots/defs/sheetPage';
import { poolsKey } from '../pools/pools';
import { useSheetToday } from '../session/session';
import { DEFAULT_FISCAL_START_MONTH, DEFAULT_FLOOR, DEFAULT_LATE_PCT, DEFAULT_TERMS, inForce, settingsKey, useSettings, type Setting } from './settings';

const GRADES = ['A', 'B', 'C'] as const;
type GradeTerms = `terms-${(typeof GRADES)[number]}`;
type GradeGrace = `grace-${(typeof GRADES)[number]}`;
type GradeProof = `proof-threshold-${(typeof GRADES)[number]}`;
type GradeClerkLimit = `clerk-limit-${(typeof GRADES)[number]}`;
const PERM_ACTIONS = ['open', 'settle', 'void', 'reassign', 'close', 'reopen'] as const;
type PermKey = `perm-${(typeof PERM_ACTIONS)[number]}`;
const PERM_KEYS: PermKey[] = PERM_ACTIONS.map((a) => `perm-${a}` as PermKey);
type Key = PermKey | 'clerk-limit' | 'cap' | 'proof-threshold' | 'note-threshold' | 'review-threshold' | 'floor' | 'terms' | GradeTerms | GradeGrace | GradeProof | GradeClerkLimit | 'late-pct' | 'rounding' | 'policy-date' | 'period-numbering-from' | 'entry-date' | 'grace-days' | 'escalate-after' | 'review-sla' | 'pot' | 'alert-threshold' | 'fiscal-start-month' | 'proof-kinds' | 'parent' | 'default-grade';
const KEYS: Key[] = ['clerk-limit', 'cap', 'proof-threshold', 'note-threshold', 'review-threshold', 'floor', 'terms', 'terms-A', 'terms-B', 'terms-C', 'grace-A', 'grace-B', 'grace-C', 'proof-threshold-A', 'proof-threshold-B', 'proof-threshold-C', 'clerk-limit-A', 'clerk-limit-B', 'clerk-limit-C', 'late-pct', 'rounding', 'policy-date', 'period-numbering-from', 'entry-date', 'grace-days', 'escalate-after', 'review-sla', 'pot', 'alert-threshold', 'fiscal-start-month', 'proof-kinds', 'parent', 'default-grade', ...PERM_KEYS];
const BLANK: Record<Key, string> = {
  'clerk-limit': '', cap: '', 'proof-threshold': '', 'note-threshold': '', 'review-threshold': '', floor: '', terms: '', 'terms-A': '', 'terms-B': '', 'terms-C': '', 'grace-A': '', 'grace-B': '', 'grace-C': '', 'proof-threshold-A': '', 'proof-threshold-B': '', 'proof-threshold-C': '', 'clerk-limit-A': '', 'clerk-limit-B': '', 'clerk-limit-C': '', 'late-pct': '', rounding: '', 'policy-date': '', 'period-numbering-from': '', 'entry-date': '', 'grace-days': '', 'escalate-after': '', 'review-sla': '', pot: '', 'alert-threshold': '', 'fiscal-start-month': '', 'proof-kinds': '', parent: '', 'default-grade': '',
  ...(Object.fromEntries(PERM_KEYS.map((k) => [k, ''])) as Record<PermKey, string>),
};
/** The keys whose values are dated: each saved from a from-date (blank: the sheet's today). */
const DATED: Key[] = ['clerk-limit', 'cap', 'proof-threshold', 'note-threshold', 'review-threshold', 'floor', 'terms', 'terms-A', 'terms-B', 'terms-C', 'grace-A', 'grace-B', 'grace-C', 'proof-threshold-A', 'proof-threshold-B', 'proof-threshold-C', 'clerk-limit-A', 'clerk-limit-B', 'clerk-limit-C', 'late-pct', 'rounding'];
const dated = (key: Key) => DATED.includes(key);

/** The sheet's settings: the clerk limit (the most a clerk may settle a claim's due for; blank for no limit) and the
 *  cap (the most a person's due-total may be; blank for none) and the proof threshold (a claim whose due is above
 *  the one in force on its date cannot be settled nor approved while it has no proofs, nor while it lacks a proof of one of the proof kinds when they are set; blank for none) and each grade's own proof threshold (a claim whose person has that grade on its date takes the grade's in force on that date instead of the sheet's; blank for none) and each grade's own clerk limit (a claim whose person has that grade on the sheet's today takes the grade's in force on it instead of the sheet's; blank for none) and the note threshold (a claim whose due is above the one in force on its date cannot be
 *  opened while its note is empty; blank for none) and the floor (the least a claim's live due may be; 1.00 until set) and the terms (the days from a claim's date to its
 *  due-by, fixed when the claim is added; 14 until set) and each grade's own terms (a grade without its own has the
 *  sheet's) and each grade's grace (whole days added to the due-by of a claim whose person has that grade on its date, the one in force on that date, before
 *  it is overdue and its late charge counts; blank for none) and the late percent (what an overdue claim gains of its figure for every full 30 days
 *  overdue; 0.00 until set) and the policy date (yyyy-MM-dd; blank: the sheet's today — a claim dated before it rounds
 *  half up, one on or after it half to even) and the rounding mode (half-up or half-even; blank: none — a claim whose date has
 *  one in force rounds its live figures by it instead of by the policy date) and the period numbering from-date (yyyy-MM-dd; blank: off — while the sheet's
 *  today is on or after it, a claim opened is numbered within its period) and the entry date (yyyy-MM-dd; blank: off — while set, a new claim given no
 *  date, by the claim form, the quick-add bar, a paste or a correction, is dated it instead of the sheet's today) and the pot (two decimals; blank: none — shared out as each balanced pool's cuts; undated) and the alert threshold (two decimals; blank: off — a person whose due-total rises above it is alerted in the audit; undated) and the fiscal year start month (1 to 12; 1 until set; undated) and the parent sheet (the id of a sheet that exists, not this one nor one with this one as an ancestor; blank: none — a grade with no rate in force on a date takes its parent's, then the parent's parent's, up to three levels up; undated) and the grace days (a whole number; blank: off — when the
 *  sheet's today changes, every open period whose to-date plus them is before the new today is closed) and the escalation gap
 *  (a whole number of days; blank: off — reminding a claim whose previous reminder was at least that many days before the
 *  sheet's today escalates it, once per claim; undated) and the review SLA (a whole number of days; blank: none — a REVIEW claim
 *  whose review day plus them is before the sheet's today is review-late; undated) and the default grade (A, B or C; blank: A — the grade a person added by the quick-add bar, or by an import line with a blank grade, takes; undated) and the permission matrix (for each of open, settle,
 *  void, reassign, close and reopen, the roles that may take it, comma separated; blank: the standing rule — open, settle,
 *  void and close by clerk and steward, reassign and reopen by steward; undated). Every setting but the policy date, the
 *  period numbering from-date, the entry date, the grace days, the escalation gap, the review SLA, the fiscal year start month, the proof kinds and the permissions is dated: saved with its from-date (blank: the sheet's today) it keeps the values from
 *  other dates, and what shows in force is the value on the sheet's today (a claim takes its floor, late percent and
 *  terms, and proof and note thresholds, from its own date). Saving stores the fields entered since the last save
 *  (a field focused counts, so a blank typed over a blank still saves), the clerk limit when none was. */
function Settings({ sheetId, user }: { sheetId: string; user: string }) {
  const settings = useSettings(sheetId);
  const queryClient = useQueryClient();
  const today = useSheetToday(sheetId);
  const [values, setValues] = useState<Record<Key, string>>(BLANK);
  const [froms, setFroms] = useState<Record<Key, string>>(BLANK);
  const touched = useRef(new Set<Key>());

  const submit = useSubmitOnce();
  const save = useMutation({
    mutationFn: async (entries: [Key, string, string][]) => {
      for (const [key, value, from] of entries) {
        await putJson<Setting>(`/api/sheets/${encodeURIComponent(sheetId)}/settings/${key}`, { value, from, by: user });
      }
    },
    onSuccess: () => {
      setValues(BLANK);
      setFroms(BLANK);
      touched.current.clear();
    },
    onSettled: (_data, _error, variables) => {
      submit.end();
      void queryClient.invalidateQueries({ queryKey: settingsKey(sheetId) });
      void queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) });
      // A new parent changes the sheet's parent and the rates it uses.
      if (variables.some(([key]) => key === 'parent')) void queryClient.invalidateQueries({ queryKey: ['sheet', sheetId] });
    },
  });
  const current = (key: Key) => (dated(key) ? inForce(settings.data, key, today) : settings.data?.find((s) => s.key === key))?.value ?? '';

  const field = (key: Key, label: string, inputMode: 'decimal' | 'numeric' | 'text' = 'decimal') => (
    <label>
      {label}{' '}
      <input
        data-testid={`setting-${key}`}
        inputMode={inputMode}
        value={values[key]}
        onFocus={() => touched.current.add(key)}
        onChange={(e) => {
          touched.current.add(key);
          const value = e.target.value;
          setValues((was) => ({ ...was, [key]: value }));
          save.reset();
        }}
      />
      {dated(key) && (
        <>
          {' '}from{' '}
          <input
            data-testid={`setting-${key}-from`}
            inputMode="numeric"
            placeholder="yyyy-MM-dd"
            value={froms[key]}
            onFocus={() => touched.current.add(key)}
            onChange={(e) => {
              touched.current.add(key);
              const from = e.target.value;
              setFroms((was) => ({ ...was, [key]: from }));
              save.reset();
            }}
          />
        </>
      )}
    </label>
  );

  return (
    <section data-testid="settings">
      <h3>Settings</h3>
      <form
        data-testid="settings-form"
        onSubmit={(e) => {
          e.preventDefault();
          if (!submit.begin()) return;
          save.reset();
          const keys = KEYS.filter((k) => touched.current.has(k));
          save.mutate((keys.length > 0 ? keys : (['clerk-limit'] as Key[])).map((k) => [k, values[k], froms[k]]));
        }}
      >
        {field('clerk-limit', 'Clerk limit')} {field('cap', 'Cap')} {field('proof-threshold', 'Proof threshold')} {field('note-threshold', 'Note threshold')} {field('review-threshold', 'Review threshold')} {field('floor', 'Floor')}{' '}
        {field('terms', 'Terms (days)', 'numeric')}{' '}
        {GRADES.map((g) => (
          <span key={g}>{field(`terms-${g}`, `Terms grade ${g} (days)`, 'numeric')} </span>
        ))}
        {GRADES.map((g) => (
          <span key={g}>{field(`grace-${g}`, `Grace grade ${g} (days)`, 'numeric')} </span>
        ))}
        {GRADES.map((g) => (
          <span key={`proof-${g}`}>{field(`proof-threshold-${g}`, `Proof threshold grade ${g}`)} </span>
        ))}
        {GRADES.map((g) => (
          <span key={`clerk-limit-${g}`}>{field(`clerk-limit-${g}`, `Clerk limit grade ${g}`)} </span>
        ))}
        {field('late-pct', 'Late %')}{' '}
        {field('rounding', 'Rounding (half-up or half-even)', 'text')}{' '}
        {field('policy-date', 'Policy date (yyyy-MM-dd)', 'numeric')}{' '}
        {field('period-numbering-from', 'Period numbering from (yyyy-MM-dd)', 'numeric')}{' '}
        {field('entry-date', 'Entry date (yyyy-MM-dd)', 'numeric')}{' '}
        {field('grace-days', 'Grace days', 'numeric')}{' '}
        {field('escalate-after', 'Escalate after (days)', 'numeric')}{' '}
        {field('review-sla', 'Review SLA (days)', 'numeric')}{' '}
        {field('pot', 'Pot')}{' '}
        {field('alert-threshold', 'Alert threshold')}{' '}
        {field('fiscal-start-month', 'Fiscal year start month (1 to 12)', 'numeric')}{' '}
        {field('proof-kinds', 'Proof kinds (receipt, photo; comma separated)', 'text')}{' '}
        {field('parent', 'Parent sheet', 'text')}{' '}
        {field('default-grade', 'Default grade (A, B or C)', 'text')}{' '}
        {PERM_ACTIONS.map((a) => (
          <span key={a}>{field(`perm-${a}`, `Roles that may ${a} (comma separated)`, 'text')} </span>
        ))}
        <button type="submit" data-testid="settings-save" disabled={save.isPending}>
          Save settings
        </button>
        {save.isError && (
          <span data-testid="settings-error">
            The from-date must be yyyy-MM-dd, or blank; the clerk limit, and a grade's clerk limit, must be an amount with up to two decimals, or blank; the cap, the proof threshold, a grade's proof threshold and the note threshold likewise; the floor an amount with up to two
            decimals; the terms, and a grade's terms, a whole number of days, 0 or more; a grade's grace a whole number of days, 0 or more, or blank; the late percent a percent with up to two decimals; the rounding half-up or half-even, or blank; the policy date yyyy-MM-dd, or blank; the period numbering from-date yyyy-MM-dd, or blank; the entry date yyyy-MM-dd, or blank; the grace days, the escalation gap and the review SLA a whole number, 0 or more, or blank; the pot an amount with up to two decimals, or blank; the alert threshold an amount with up to two decimals, or blank; the fiscal year start month a whole month 1 to 12; the proof kinds kinds (receipt, photo) separated by commas, or blank; a permission roles (clerk, steward) separated by commas, or blank; the default grade A, B or C, or blank; the parent a sheet that exists (has been loaded), not this sheet nor one that has this sheet as an ancestor, or blank.
          </span>
        )}
      </form>
      {settings.isError && <p data-testid="settings-load-error">Could not load settings.</p>}
      <p>
        Clerk limit in force <span data-testid="setting-clerk-limit-current">{current('clerk-limit')}</span>
      </p>
      <p>
        Cap in force <span data-testid="setting-cap-current">{current('cap')}</span>
      </p>
      <p>
        Proof threshold in force <span data-testid="setting-proof-threshold-current">{current('proof-threshold')}</span>
      </p>
      <p>
        Note threshold in force <span data-testid="setting-note-threshold-current">{current('note-threshold')}</span>
      </p>
      <p>
        Review threshold in force <span data-testid="setting-review-threshold-current">{current('review-threshold')}</span>
      </p>
      <p>
        Floor in force <span data-testid="setting-floor-current">{current('floor') || DEFAULT_FLOOR}</span>
      </p>
      <p>
        Terms in force <span data-testid="setting-terms-current">{current('terms') || DEFAULT_TERMS}</span>
      </p>
      {GRADES.map((g) => (
        <p key={g}>
          Terms of grade {g} in force{' '}
          <span data-testid={`setting-terms-${g}-current`}>{current(`terms-${g}`) || current('terms') || DEFAULT_TERMS}</span>
        </p>
      ))}
      {GRADES.map((g) => (
        <p key={`grace-${g}`}>
          Grace of grade {g} in force <span data-testid={`setting-grace-${g}-current`}>{current(`grace-${g}`)}</span>
        </p>
      ))}
      {GRADES.map((g) => (
        <p key={`proof-threshold-${g}`}>
          Proof threshold of grade {g} in force{' '}
          <span data-testid={`setting-proof-threshold-${g}-current`}>{current(`proof-threshold-${g}`)}</span>
        </p>
      ))}
      {GRADES.map((g) => (
        <p key={`clerk-limit-${g}`}>
          Clerk limit of grade {g} in force{' '}
          <span data-testid={`setting-clerk-limit-${g}-current`}>{current(`clerk-limit-${g}`)}</span>
        </p>
      ))}
      <p>
        Late percent in force{' '}
        <span data-testid="setting-late-pct-current">{current('late-pct') || DEFAULT_LATE_PCT}</span>
      </p>
      <p>
        Rounding in force <span data-testid="setting-rounding-current">{current('rounding')}</span>
      </p>
      <p>
        Policy date <span data-testid="setting-policy-date-current">{current('policy-date')}</span>
      </p>
      <p>
        Period numbering from{' '}
        <span data-testid="setting-period-numbering-from-current">{current('period-numbering-from')}</span>
      </p>
      <p>
        Entry date <span data-testid="setting-entry-date-current">{current('entry-date')}</span>
      </p>
      <p>
        Grace days <span data-testid="setting-grace-days-current">{current('grace-days')}</span>
      </p>
      <p>
        Escalate after <span data-testid="setting-escalate-after-current">{current('escalate-after')}</span>
      </p>
      <p>
        Review SLA <span data-testid="setting-review-sla-current">{current('review-sla')}</span>
      </p>
      <p>
        Pot <span data-testid="setting-pot-current">{current('pot')}</span>
      </p>
      <p>
        Alert threshold <span data-testid="setting-alert-threshold-current">{current('alert-threshold')}</span>
      </p>
      <p>
        Proof kinds <span data-testid="setting-proof-kinds-current">{current('proof-kinds')}</span>
      </p>
      <p>
        Fiscal year start month{' '}
        <span data-testid="setting-fiscal-start-month-current">{current('fiscal-start-month') || DEFAULT_FISCAL_START_MONTH}</span>
      </p>
      <p>
        Parent sheet <span data-testid="setting-parent-current">{current('parent')}</span>
      </p>
      <p>
        Default grade <span data-testid="setting-default-grade-current">{current('default-grade')}</span>
      </p>
      {PERM_ACTIONS.map((a) => (
        <p key={a}>
          Roles that may {a}{' '}
          <span data-testid={`setting-perm-${a}-current`}>{current(`perm-${a}`)}</span>
        </p>
      ))}
    </section>
  );
}

export const contribution = SheetPage.fill({ order: 6, Component: Settings });
