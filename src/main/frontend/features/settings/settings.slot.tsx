import { useRef, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { SheetPage } from '../../slots/defs/sheetPage';
import { useSheetToday } from '../session/session';
import { DEFAULT_FLOOR, DEFAULT_LATE_PCT, DEFAULT_TERMS, inForce, settingsKey, useSettings, type Setting } from './settings';

const GRADES = ['A', 'B', 'C'] as const;
type GradeTerms = `terms-${(typeof GRADES)[number]}`;
const PERM_ACTIONS = ['open', 'settle', 'void', 'reassign', 'close', 'reopen'] as const;
type PermKey = `perm-${(typeof PERM_ACTIONS)[number]}`;
const PERM_KEYS: PermKey[] = PERM_ACTIONS.map((a) => `perm-${a}` as PermKey);
type Key = PermKey | 'clerk-limit' | 'cap' | 'proof-threshold' | 'floor' | 'terms' | GradeTerms | 'late-pct' | 'policy-date' | 'period-numbering-from' | 'grace-days';
const KEYS: Key[] = ['clerk-limit', 'cap', 'proof-threshold', 'floor', 'terms', 'terms-A', 'terms-B', 'terms-C', 'late-pct', 'policy-date', 'period-numbering-from', 'grace-days', ...PERM_KEYS];
const BLANK: Record<Key, string> = {
  'clerk-limit': '', cap: '', 'proof-threshold': '', floor: '', terms: '', 'terms-A': '', 'terms-B': '', 'terms-C': '', 'late-pct': '', 'policy-date': '', 'period-numbering-from': '', 'grace-days': '',
  ...(Object.fromEntries(PERM_KEYS.map((k) => [k, ''])) as Record<PermKey, string>),
};
/** The keys whose values are dated: each saved from a from-date (blank: the sheet's today). */
const DATED: Key[] = ['clerk-limit', 'cap', 'proof-threshold', 'floor', 'terms', 'terms-A', 'terms-B', 'terms-C', 'late-pct'];
const dated = (key: Key) => DATED.includes(key);

/** The sheet's settings: the clerk limit (the most a clerk may settle a claim's due for; blank for no limit) and the
 *  cap (the most a person's due-total may be; blank for none) and the proof threshold (a claim whose due is above
 *  the one in force on its date cannot be settled nor approved while it has no proofs; blank for none) and the floor (the least a claim's live due may be; 1.00 until set) and the terms (the days from a claim's date to its
 *  due-by, fixed when the claim is added; 14 until set) and each grade's own terms (a grade without its own has the
 *  sheet's) and the late percent (what an overdue claim gains of its figure for every full 30 days
 *  overdue; 0.00 until set) and the policy date (yyyy-MM-dd; blank: the sheet's today — a claim dated before it rounds
 *  half up, one on or after it half to even) and the period numbering from-date (yyyy-MM-dd; blank: off — while the sheet's
 *  today is on or after it, a claim opened is numbered within its period) and the grace days (a whole number; blank: off — when the
 *  sheet's today changes, every open period whose to-date plus them is before the new today is closed) and the permission matrix (for each of open, settle,
 *  void, reassign, close and reopen, the roles that may take it, comma separated; blank: the standing rule — open, settle,
 *  void and close by clerk and steward, reassign and reopen by steward; undated). Every setting but the policy date, the
 *  period numbering from-date, the grace days and the permissions is dated: saved with its from-date (blank: the sheet's today) it keeps the values from
 *  other dates, and what shows in force is the value on the sheet's today (a claim takes its floor, late percent and
 *  terms, and proof threshold, from its own date). Saving stores the fields entered since the last save
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
    onSettled: () => {
      submit.end();
      void queryClient.invalidateQueries({ queryKey: settingsKey(sheetId) });
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
        {field('clerk-limit', 'Clerk limit')} {field('cap', 'Cap')} {field('proof-threshold', 'Proof threshold')} {field('floor', 'Floor')}{' '}
        {field('terms', 'Terms (days)', 'numeric')}{' '}
        {GRADES.map((g) => (
          <span key={g}>{field(`terms-${g}`, `Terms grade ${g} (days)`, 'numeric')} </span>
        ))}
        {field('late-pct', 'Late %')}{' '}
        {field('policy-date', 'Policy date (yyyy-MM-dd)', 'numeric')}{' '}
        {field('period-numbering-from', 'Period numbering from (yyyy-MM-dd)', 'numeric')}{' '}
        {field('grace-days', 'Grace days', 'numeric')}{' '}
        {PERM_ACTIONS.map((a) => (
          <span key={a}>{field(`perm-${a}`, `Roles that may ${a} (comma separated)`, 'text')} </span>
        ))}
        <button type="submit" data-testid="settings-save" disabled={save.isPending}>
          Save settings
        </button>
        {save.isError && (
          <span data-testid="settings-error">
            The from-date must be yyyy-MM-dd, or blank; the clerk limit must be an amount with up to two decimals, or blank; the cap and the proof threshold likewise; the floor an amount with up to two
            decimals; the terms, and a grade's terms, a whole number of days, 0 or more; the late percent a percent with up to two decimals; the policy date yyyy-MM-dd, or blank; the period numbering from-date yyyy-MM-dd, or blank; the grace days a whole number, 0 or more, or blank; a permission roles (clerk, steward) separated by commas, or blank.
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
      <p>
        Grace days <span data-testid="setting-grace-days-current">{current('grace-days')}</span>
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
