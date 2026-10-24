import { Fragment, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { settingsPath, settingsQueryKey, useSettings, type Settings } from './roles';

type ValueKey = 'clerkLimit' | 'floor' | 'terms' | 'latePct' | 'policyDate' | 'periodNumberingFrom' | 'termsA' | 'termsB' | 'termsC' | 'cap' | 'proofThreshold' | PermKey;

/** The permission matrix: for each action, the roles that may take it (comma separated; blank: the standing rule).
 *  Not dated. */
const PERMS = [
  { action: 'open', key: 'permOpen', standing: 'clerk,steward' },
  { action: 'settle', key: 'permSettle', standing: 'clerk,steward' },
  { action: 'void', key: 'permVoid', standing: 'clerk,steward' },
  { action: 'reassign', key: 'permReassign', standing: 'steward' },
  { action: 'close', key: 'permClose', standing: 'clerk,steward' },
  { action: 'reopen', key: 'permReopen', standing: 'steward' },
] as const;
type PermKey = (typeof PERMS)[number]['key'];

/** The settings that carry a figure: each is saved from a from-date (blank: the sheet's today). */
const DATED = ['clerkLimit', 'floor', 'terms', 'latePct', 'termsA', 'termsB', 'termsC', 'cap', 'proofThreshold'] as const;
type DatedKey = (typeof DATED)[number];
type FromKey = `${DatedKey}From`;
type Key = ValueKey | FromKey;

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
// a claim's due-by is fixed when it is created, from the terms of its person's grade on its date), the cap (a person's due-total is at most it; blank: none), the proof threshold (a claim whose due is above the one in force on its date cannot be settled or approved while it has no proof; blank: none), the permission matrix (for each of open, settle, void, reassign, close and reopen, the roles that may take it; blank: the standing rule — open, settle, void and close by clerk and steward, reassign and reopen by steward; not dated), and the form that sets them. A clerk settles a claim only when its due is at or below the
// limit; a steward settles anything. Each field shows the value in force until it is typed in (or focused); saving sets only
// the fields typed in (all of them when none was). Each setting that carries a figure is saved from its from-date (blank:
// the sheet's today), keeping the values recorded from other dates; each shown is the one in force on the sheet's today.
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
  const current: Record<ValueKey, string> = {
    clerkLimit: settings.data?.clerkLimit ?? '',
    floor: settings.data?.floor ?? '',
    terms: settings.data?.terms ?? '',
    latePct: settings.data?.latePct ?? '',
    policyDate: settings.data?.policyDate ?? '',
    periodNumberingFrom: settings.data?.periodNumberingFrom ?? '',
    termsA: settings.data?.termsA ?? '',
    termsB: settings.data?.termsB ?? '',
    termsC: settings.data?.termsC ?? '',
    cap: settings.data?.cap ?? '',
    proofThreshold: settings.data?.proofThreshold ?? '',
    ...(Object.fromEntries(PERMS.map(({ key }) => [key, settings.data?.[key] ?? ''])) as Record<PermKey, string>),
  };
  const valueOf = (key: Key) => drafts[key] ?? (key in current ? current[key as ValueKey] : '');
  const edit = (key: Key) => (e: { currentTarget: HTMLInputElement }) => {
    const value = e.currentTarget.value;
    setDrafts((d) => ({ ...d, [key]: value }));
    save.reset();
  };
  // A field focused counts as typed in, even when left as it is (filling a blank field with blank changes nothing).
  const touch = (key: Key) => () => setDrafts((d) => (key in d ? d : { ...d, [key]: valueOf(key) }));
  // The from-date field of a setting that carries a figure, after its value field.
  const fromField = (key: DatedKey, testid: string) => (
    <input
      data-testid={`${testid}-from`}
      placeholder="From YYYY-MM-DD (blank: today)"
      value={valueOf(`${key}From`)}
      onChange={edit(`${key}From`)}
      onInput={edit(`${key}From`)}
      onFocus={touch(`${key}From`)}
    />
  );
  // What a save sends: the fields typed in (all the values when none was); a from-date typed in sends its setting's
  // value with it.
  const body = (): Drafts => {
    if (Object.keys(drafts).length === 0) return { ...current };
    const sent: Drafts = { ...drafts };
    for (const key of DATED) {
      if ((sent[`${key}From`] ?? '') !== '' && sent[key] === undefined) sent[key] = current[key];
    }
    return sent;
  };
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
      <p>
        Cap: <span data-testid="setting-cap-current">{settings.data?.cap ?? ''}</span>
      </p>
      <p>
        Proof threshold:{' '}
        <span data-testid="setting-proof-threshold-current">{settings.data?.proofThreshold ?? ''}</span>
      </p>
      {PERMS.map(({ action, key, standing }) => (
        <p key={action}>
          Roles that may {action} (blank: {standing}):{' '}
          <span data-testid={`setting-perm-${action}-current`}>{settings.data?.[key] ?? ''}</span>
        </p>
      ))}
      <form
        data-testid="settings-form"
        onSubmit={(e) => {
          e.preventDefault();
          submit(body());
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
        {fromField('clerkLimit', 'setting-clerk-limit')}
        <input
          data-testid="setting-floor"
          placeholder="Floor (blank: 1.00)"
          inputMode="decimal"
          value={valueOf('floor')}
          onChange={edit('floor')}
          onInput={edit('floor')}
          onFocus={touch('floor')}
        />
        {fromField('floor', 'setting-floor')}
        <input
          data-testid="setting-terms"
          placeholder="Terms in days (blank: 14)"
          inputMode="numeric"
          value={valueOf('terms')}
          onChange={edit('terms')}
          onInput={edit('terms')}
          onFocus={touch('terms')}
        />
        {fromField('terms', 'setting-terms')}
        <input
          data-testid="setting-late-pct"
          placeholder="Late percent (blank: 0.00)"
          inputMode="decimal"
          value={valueOf('latePct')}
          onChange={edit('latePct')}
          onInput={edit('latePct')}
          onFocus={touch('latePct')}
        />
        {fromField('latePct', 'setting-late-pct')}
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
          <Fragment key={grade}>
            <input
              data-testid={`setting-terms-${grade}`}
              placeholder={`Terms of grade ${grade} in days (blank: the sheet's)`}
              inputMode="numeric"
              value={valueOf(key)}
              onChange={edit(key)}
              onInput={edit(key)}
              onFocus={touch(key)}
            />
            {fromField(key, `setting-terms-${grade}`)}
          </Fragment>
        ))}
        <input
          data-testid="setting-cap"
          placeholder="Cap (blank: none)"
          inputMode="decimal"
          value={valueOf('cap')}
          onChange={edit('cap')}
          onInput={edit('cap')}
          onFocus={touch('cap')}
        />
        {fromField('cap', 'setting-cap')}
        <input
          data-testid="setting-proof-threshold"
          placeholder="Proof threshold (blank: none)"
          inputMode="decimal"
          value={valueOf('proofThreshold')}
          onChange={edit('proofThreshold')}
          onInput={edit('proofThreshold')}
          onFocus={touch('proofThreshold')}
        />
        {fromField('proofThreshold', 'setting-proof-threshold')}
        {PERMS.map(({ action, key, standing }) => (
          <input
            key={action}
            data-testid={`setting-perm-${action}`}
            placeholder={`Roles that may ${action}, comma separated (blank: ${standing})`}
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
          <span data-testid="settings-error">The clerk limit and floor must each be an amount, 0.00 or more, with up to two decimals, or blank; the terms a whole number of days, or blank; the late percent a percent, 0.00 or more, with up to two decimals, or blank; the policy date and period numbering from each a date (YYYY-MM-DD), or blank; a grade's terms a whole number of days, or blank; the cap and proof threshold each an amount, 0.00 or more, with up to two decimals, or blank; a from-date a date (YYYY-MM-DD), or blank; the roles that may take an action a comma separated list of clerk and steward, or blank.</span>
        )}
      </form>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 8, Component: SheetSettings });
