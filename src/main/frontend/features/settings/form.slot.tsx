import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { type Settings, settingsKey, useSettings } from './settings';

const PERM_ACTIONS = ['open', 'settle', 'void', 'reassign', 'close', 'reopen'] as const;

/** The standing rule each action keeps while its permission is blank. */
const STANDING_PERMS: Record<(typeof PERM_ACTIONS)[number], string> = {
  open: 'clerk,steward', settle: 'clerk,steward', void: 'clerk,steward', reassign: 'steward', close: 'clerk,steward', reopen: 'steward',
};

type PermKey = `perm-${(typeof PERM_ACTIONS)[number]}`;

type Key = 'clerk-limit' | 'floor' | 'terms' | 'terms-A' | 'terms-B' | 'terms-C' | 'late-pct' | 'policy-date' | 'period-numbering-from' | 'cap' | 'proof-threshold' | 'grace-days' | PermKey;

const GRADES = ['A', 'B', 'C'] as const;

/** The settings that carry a figure: each is saved from a from-date (blank for the sheet's today). */
type DatedKey = Exclude<Key, 'policy-date' | 'period-numbering-from' | 'grace-days' | PermKey>;

const NO_FROM: Record<DatedKey, string> = { 'clerk-limit': '', floor: '', terms: '', 'terms-A': '', 'terms-B': '', 'terms-C': '', 'late-pct': '', cap: '', 'proof-threshold': '' };

const isDated = (k: Key): k is DatedKey => k in NO_FROM;

// The sheet's settings in force, and the form that changes them: the clerk limit (blank for none),
// the floor (blank for its default), the terms in days (blank for 14), each grade's own terms (blank
// for the sheet's terms) and the late percent (blank for
// 0.00), the policy date rounding turns on (blank for the sheet's today) and the date claims opened
// are numbered per period from (blank for off), and the cap on a person's due-total (blank for none), and the proof threshold above which a claim's due needs a proof to be settled or approved (blank for none). Saving changes the setting last touched, so a field left blank does not clear the others.
// Each setting that carries a figure is saved from its from-date (blank for the sheet's today), and shows
// the value in force on the sheet's today; the policy date and period numbering date are not dated.
// The permission matrix gives, for each action, the roles that may take it, comma separated (blank keeps
// the standing rule, shown as the placeholder); it is not dated. The grace days (blank for off, not
// dated) are the whole days after its to-date an open period is closed when the sheet's today moves.
function SheetSettings() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const settings = useSettings(sheet);
  const [values, setValues] = useState<Record<Key, string>>({ 'clerk-limit': '', floor: '', terms: '', 'terms-A': '', 'terms-B': '', 'terms-C': '', 'late-pct': '', 'policy-date': '', 'period-numbering-from': '', cap: '', 'proof-threshold': '', 'grace-days': '',
    'perm-open': '', 'perm-settle': '', 'perm-void': '', 'perm-reassign': '', 'perm-close': '', 'perm-reopen': '' });
  const [froms, setFroms] = useState<Record<DatedKey, string>>(NO_FROM);
  const [key, setKey] = useState<Key>('clerk-limit');
  const queryClient = useQueryClient();

  const save = useMutation({
    mutationFn: ({ key, value, from }: { key: Key; value: string; from: string }) =>
      postJson<Settings>(`/api/sheets/${encodeURIComponent(sheet)}/settings`, { key, value, from, user }),
    onSuccess: async (_, { key }) => {
      await queryClient.invalidateQueries({ queryKey: settingsKey(sheet) });
      setValues((v) => ({ ...v, [key]: '' }));
      if (isDated(key)) setFroms((f) => ({ ...f, [key]: '' }));
    },
  });
  const submit = useSubmitOnce(save);

  const field = (k: Key, placeholder: string, inputMode: 'decimal' | 'text' = 'decimal') => (
    <input
      data-testid={`setting-${k}`}
      placeholder={placeholder}
      inputMode={inputMode}
      value={values[k]}
      onFocus={() => setKey(k)}
      onChange={(e) => {
        setValues({ ...values, [k]: e.target.value });
        setKey(k);
        save.reset();
      }}
    />
  );

  const from = (k: DatedKey) => (
    <input
      data-testid={`setting-${k}-from`}
      placeholder="From YYYY-MM-DD (blank for today)"
      value={froms[k]}
      onFocus={() => setKey(k)}
      onChange={(e) => {
        setFroms({ ...froms, [k]: e.target.value });
        setKey(k);
        save.reset();
      }}
    />
  );

  return (
    <section data-testid="settings">
      <h2>Settings</h2>
      <form
        data-testid="settings-form"
        onSubmit={(e) => {
          e.preventDefault();
          submit({ key, value: values[key], from: isDated(key) ? froms[key] : '' });
        }}
      >
        Clerk limit <strong data-testid="setting-clerk-limit-current">{settings.data?.clerkLimit ?? ''}</strong>{' '}
        {field('clerk-limit', 'Clerk limit (blank for none)')}{' '}{from('clerk-limit')}{' '}
        Floor <strong data-testid="setting-floor-current">{settings.data?.floor ?? ''}</strong>{' '}
        {field('floor', 'Floor (blank for 1.00)')}{' '}{from('floor')}{' '}
        Terms <strong data-testid="setting-terms-current">{settings.data?.terms ?? ''}</strong>{' '}
        {field('terms', 'Terms in days (blank for 14)')}{' '}{from('terms')}{' '}
        {GRADES.map((g) => (
          <span key={g}>
            Terms {g} <strong data-testid={`setting-terms-${g}-current`}>{settings.data?.gradeTerms?.[g] ?? ''}</strong>{' '}
            {field(`terms-${g}`, `Terms for grade ${g} (blank for the sheet's)`)}{' '}{from(`terms-${g}`)}{' '}
          </span>
        ))}
        Late percent <strong data-testid="setting-late-pct-current">{settings.data?.latePct ?? ''}</strong>{' '}
        {field('late-pct', 'Late percent (blank for 0.00)')}{' '}{from('late-pct')}{' '}
        Policy date <strong data-testid="setting-policy-date-current">{settings.data?.policyDate ?? ''}</strong>{' '}
        {field('policy-date', 'Policy date YYYY-MM-DD (blank for today)', 'text')}{' '}
        Period numbering from <strong data-testid="setting-period-numbering-from-current">{settings.data?.periodNumberingFrom ?? ''}</strong>{' '}
        {field('period-numbering-from', 'Period numbering from YYYY-MM-DD (blank for off)', 'text')}{' '}
        Cap <strong data-testid="setting-cap-current">{settings.data?.cap ?? ''}</strong>{' '}
        {field('cap', 'Cap (blank for none)')}{' '}{from('cap')}{' '}
        Proof threshold <strong data-testid="setting-proof-threshold-current">{settings.data?.proofThreshold ?? ''}</strong>{' '}
        {field('proof-threshold', 'Proof threshold (blank for none)')}{' '}{from('proof-threshold')}{' '}
        Grace days <strong data-testid="setting-grace-days-current">{settings.data?.graceDays ?? ''}</strong>{' '}
        {field('grace-days', 'Grace days (blank for off)')}{' '}
        {PERM_ACTIONS.map((a) => (
          <span key={a}>
            May {a} <strong data-testid={`setting-perm-${a}-current`}>{settings.data?.perms?.[a] ?? ''}</strong>{' '}
            {field(`perm-${a}`, `Roles that may ${a}, comma separated (blank for ${STANDING_PERMS[a]})`, 'text')}{' '}
          </span>
        ))}
        <button type="submit" data-testid="settings-save" disabled={save.isPending}>
          Save settings
        </button>
        {save.isError && (
          <span data-testid="settings-error" role="alert">
            {' '}
            {save.error instanceof HttpError && save.error.reason ? `${save.error.reason}.` : 'The settings were refused.'}
          </span>
        )}
      </form>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 48, Component: SheetSettings });
