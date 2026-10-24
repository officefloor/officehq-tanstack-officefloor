import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { settingsPath, settingsQueryKey, useSettings, type Settings } from './roles';

type Key = 'clerkLimit' | 'floor' | 'terms' | 'latePct' | 'policyDate' | 'periodNumberingFrom' | 'termsA' | 'termsB' | 'termsC';

/** The grades that may have their own terms, and the setting key of each. */
const GRADE_TERMS = [
  { grade: 'A', key: 'termsA' },
  { grade: 'B', key: 'termsB' },
  { grade: 'C', key: 'termsC' },
] as const;
type Drafts = Partial<Record<Key, string>>;

// The sheet's settings: the clerk limit in force (blank when there is none), the floor in force (a claim's live
// due is at least it), the terms in force (a claim is due by its date plus that many days), the late percent in force (an overdue
// claim's due gains it of its figure for every full 30 days overdue), the policy date (a claim dated before it rounds half
// up, one on or after it half to even; blank: the sheet's today), the period numbering from (while the sheet's today is on or after it, a claim
// opened is numbered within its period: P1-1, X-1 for no period; blank: off), each grade's own terms (blank: the sheet's terms;
// a claim's due-by is fixed when it is created, from the terms of its person's grade on its date), and the form that sets them. A clerk settles a claim only when its due is at or below the
// limit; a steward settles anything. Each field shows the value in force until it is typed in (or focused); saving sets only
// the fields typed in (all of them when none was).
function SheetSettings() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const settings = useSettings();
  const [drafts, setDrafts] = useState<Drafts>({});
  const queryClient = useQueryClient();
  const save = useMutation({
    mutationFn: (body: Drafts) => putJson<Settings>(settingsPath(sheet ?? ''), body, user),
    onSuccess: async (_saved, sent) => {
      await queryClient.invalidateQueries({ queryKey: settingsQueryKey(sheet ?? '') });
      // Keep what was typed in while the save was on its way; drop only the drafts it saved.
      setDrafts((d) => Object.fromEntries(Object.entries(d).filter(([k, v]) => sent[k as Key] !== v)) as Drafts);
    },
  });
  const submit = useSubmitOnce(save);

  if (!user || !sheet) return null;
  const current: Record<Key, string> = {
    clerkLimit: settings.data?.clerkLimit ?? '',
    floor: settings.data?.floor ?? '',
    terms: settings.data?.terms ?? '',
    latePct: settings.data?.latePct ?? '',
    policyDate: settings.data?.policyDate ?? '',
    periodNumberingFrom: settings.data?.periodNumberingFrom ?? '',
    termsA: settings.data?.termsA ?? '',
    termsB: settings.data?.termsB ?? '',
    termsC: settings.data?.termsC ?? '',
  };
  const valueOf = (key: Key) => drafts[key] ?? current[key];
  const edit = (key: Key) => (e: { currentTarget: HTMLInputElement }) => {
    const value = e.currentTarget.value;
    setDrafts((d) => ({ ...d, [key]: value }));
    save.reset();
  };
  // A field focused counts as typed in, even when left as it is (filling a blank field with blank changes nothing).
  const touch = (key: Key) => () => setDrafts((d) => (key in d ? d : { ...d, [key]: current[key] }));
  return (
    <section data-testid="settings">
      <h2>Settings</h2>
      <p>
        Clerk limit: <span data-testid="setting-clerk-limit-current">{settings.data?.clerkLimit ?? ''}</span>
      </p>
      <p>
        Floor: <span data-testid="setting-floor-current">{settings.data?.floor ?? ''}</span>
      </p>
      <p>
        Terms (days): <span data-testid="setting-terms-current">{settings.data?.terms ?? ''}</span>
      </p>
      <p>
        Late percent: <span data-testid="setting-late-pct-current">{settings.data?.latePct ?? ''}</span>
      </p>
      <p>
        Policy date: <span data-testid="setting-policy-date-current">{settings.data?.policyDate ?? ''}</span>
      </p>
      <p>
        Period numbering from:{' '}
        <span data-testid="setting-period-numbering-from-current">{settings.data?.periodNumberingFrom ?? ''}</span>
      </p>
      {GRADE_TERMS.map(({ grade, key }) => (
        <p key={grade}>
          Terms of grade {grade} (days; blank: the sheet's):{' '}
          <span data-testid={`setting-terms-${grade}-current`}>{settings.data?.[key] ?? ''}</span>
        </p>
      ))}
      <form
        data-testid="settings-form"
        onSubmit={(e) => {
          e.preventDefault();
          submit(Object.keys(drafts).length > 0 ? drafts : { ...current });
        }}
      >
        <input
          data-testid="setting-clerk-limit"
          placeholder="Clerk limit (blank: no limit)"
          inputMode="decimal"
          value={valueOf('clerkLimit')}
          onChange={edit('clerkLimit')}
          onInput={edit('clerkLimit')}
          onFocus={touch('clerkLimit')}
        />
        <input
          data-testid="setting-floor"
          placeholder="Floor (blank: 1.00)"
          inputMode="decimal"
          value={valueOf('floor')}
          onChange={edit('floor')}
          onInput={edit('floor')}
          onFocus={touch('floor')}
        />
        <input
          data-testid="setting-terms"
          placeholder="Terms in days (blank: 14)"
          inputMode="numeric"
          value={valueOf('terms')}
          onChange={edit('terms')}
          onInput={edit('terms')}
          onFocus={touch('terms')}
        />
        <input
          data-testid="setting-late-pct"
          placeholder="Late percent (blank: 0.00)"
          inputMode="decimal"
          value={valueOf('latePct')}
          onChange={edit('latePct')}
          onInput={edit('latePct')}
          onFocus={touch('latePct')}
        />
        <input
          data-testid="setting-policy-date"
          placeholder="Policy date YYYY-MM-DD (blank: today)"
          value={valueOf('policyDate')}
          onChange={edit('policyDate')}
          onInput={edit('policyDate')}
          onFocus={touch('policyDate')}
        />
        <input
          data-testid="setting-period-numbering-from"
          placeholder="Period numbering from YYYY-MM-DD (blank: off)"
          value={valueOf('periodNumberingFrom')}
          onChange={edit('periodNumberingFrom')}
          onInput={edit('periodNumberingFrom')}
          onFocus={touch('periodNumberingFrom')}
        />
        {GRADE_TERMS.map(({ grade, key }) => (
          <input
            key={grade}
            data-testid={`setting-terms-${grade}`}
            placeholder={`Terms of grade ${grade} in days (blank: the sheet's)`}
            inputMode="numeric"
            value={valueOf(key)}
            onChange={edit(key)}
            onInput={edit(key)}
            onFocus={touch(key)}
          />
        ))}
        <button type="submit" data-testid="settings-save" disabled={save.isPending}>
          Save settings
        </button>
        {save.isError && (
          <span data-testid="settings-error">The clerk limit and floor must each be an amount, 0.00 or more, with up to two decimals, or blank; the terms a whole number of days, or blank; the late percent a percent, 0.00 or more, with up to two decimals, or blank; the policy date and period numbering from each a date (YYYY-MM-DD), or blank; a grade's terms a whole number of days, or blank.</span>
        )}
      </form>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 8, Component: SheetSettings });
