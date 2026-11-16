import { useRef, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { SheetPage } from '../../slots/defs/sheetPage';
import { DEFAULT_FLOOR, DEFAULT_LATE_PCT, DEFAULT_TERMS, settingsKey, useSettings, type Setting } from './settings';

const GRADES = ['A', 'B', 'C'] as const;
type GradeTerms = `terms-${(typeof GRADES)[number]}`;
type Key = 'clerk-limit' | 'floor' | 'terms' | GradeTerms | 'late-pct' | 'policy-date' | 'period-numbering-from';
const KEYS: Key[] = ['clerk-limit', 'floor', 'terms', 'terms-A', 'terms-B', 'terms-C', 'late-pct', 'policy-date', 'period-numbering-from'];
const BLANK: Record<Key, string> = {
  'clerk-limit': '', floor: '', terms: '', 'terms-A': '', 'terms-B': '', 'terms-C': '', 'late-pct': '', 'policy-date': '', 'period-numbering-from': '',
};

/** The sheet's settings: the clerk limit (the most a clerk may settle a claim's due for; blank for no limit) and the
 *  floor (the least a claim's live due may be; 1.00 until set) and the terms (the days from a claim's date to its
 *  due-by, fixed when the claim is added; 14 until set) and each grade's own terms (a grade without its own has the
 *  sheet's) and the late percent (what an overdue claim gains of its figure for every full 30 days
 *  overdue; 0.00 until set) and the policy date (yyyy-MM-dd; blank: the sheet's today — a claim dated before it rounds
 *  half up, one on or after it half to even) and the period numbering from-date (yyyy-MM-dd; blank: off — while the sheet's
 *  today is on or after it, a claim opened is numbered within its period). Saving stores the fields entered since the last save
 *  (a field focused counts, so a blank typed over a blank still saves), the clerk limit when none was. */
function Settings({ sheetId, user }: { sheetId: string; user: string }) {
  const settings = useSettings(sheetId);
  const queryClient = useQueryClient();
  const [values, setValues] = useState<Record<Key, string>>(BLANK);
  const touched = useRef(new Set<Key>());

  const submit = useSubmitOnce();
  const save = useMutation({
    mutationFn: async (entries: [Key, string][]) => {
      for (const [key, value] of entries) {
        await putJson<Setting>(`/api/sheets/${encodeURIComponent(sheetId)}/settings/${key}`, { value, by: user });
      }
    },
    onSuccess: () => {
      setValues(BLANK);
      touched.current.clear();
    },
    onSettled: () => {
      submit.end();
      void queryClient.invalidateQueries({ queryKey: settingsKey(sheetId) });
    },
  });
  const current = (key: Key) => settings.data?.find((s) => s.key === key)?.value ?? '';

  const field = (key: Key, label: string, inputMode: 'decimal' | 'numeric' = 'decimal') => (
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
          save.mutate((keys.length > 0 ? keys : (['clerk-limit'] as Key[])).map((k) => [k, values[k]]));
        }}
      >
        {field('clerk-limit', 'Clerk limit')} {field('floor', 'Floor')}{' '}
        {field('terms', 'Terms (days)', 'numeric')}{' '}
        {GRADES.map((g) => (
          <span key={g}>{field(`terms-${g}`, `Terms grade ${g} (days)`, 'numeric')} </span>
        ))}
        {field('late-pct', 'Late %')}{' '}
        {field('policy-date', 'Policy date (yyyy-MM-dd)', 'numeric')}{' '}
        {field('period-numbering-from', 'Period numbering from (yyyy-MM-dd)', 'numeric')}{' '}
        <button type="submit" data-testid="settings-save" disabled={save.isPending}>
          Save settings
        </button>
        {save.isError && (
          <span data-testid="settings-error">
            The clerk limit must be an amount with up to two decimals, or blank; the floor an amount with up to two
            decimals; the terms, and a grade's terms, a whole number of days, 0 or more; the late percent a percent with up to two decimals; the policy date yyyy-MM-dd, or blank; the period numbering from-date yyyy-MM-dd, or blank.
          </span>
        )}
      </form>
      {settings.isError && <p data-testid="settings-load-error">Could not load settings.</p>}
      <p>
        Clerk limit in force <span data-testid="setting-clerk-limit-current">{current('clerk-limit')}</span>
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
      <p>
        Late percent in force{' '}
        <span data-testid="setting-late-pct-current">{current('late-pct') || DEFAULT_LATE_PCT}</span>
      </p>
      <p>
        Policy date <span data-testid="setting-policy-date-current">{current('policy-date')}</span>
      </p>
      <p>
        Period numbering from{' '}
        <span data-testid="setting-period-numbering-from-current">{current('period-numbering-from')}</span>
      </p>
    </section>
  );
}

export const contribution = SheetPage.fill({ order: 6, Component: Settings });
