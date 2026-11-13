import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { poolsKey } from '../pools/pools';
import { ratesKey } from '../rates/rates';
import { type Settings, settingsKey, useSettings } from './settings';
import { useFigure } from '../locale/locale';

const PERM_ACTIONS = ['open', 'settle', 'void', 'reassign', 'close', 'reopen', 'today'] as const;

/** The standing rule each action keeps while its permission is blank. */
const STANDING_PERMS: Record<(typeof PERM_ACTIONS)[number], string> = {
  open: 'clerk,steward', settle: 'clerk,steward', void: 'clerk,steward', reassign: 'steward', close: 'clerk,steward', reopen: 'steward', today: 'clerk,steward',
};

type PermKey = `perm-${(typeof PERM_ACTIONS)[number]}`;

type Key = 'clerk-limit' | 'clerk-limit-A' | 'clerk-limit-B' | 'clerk-limit-C' | 'floor' | 'terms' | 'terms-A' | 'terms-B' | 'terms-C' | 'grace-A' | 'grace-B' | 'grace-C' | 'late-pct' | 'policy-date' | 'period-numbering-from' | 'cap' | 'proof-threshold' | 'proof-threshold-A' | 'proof-threshold-B' | 'proof-threshold-C' | 'note-threshold' | 'review-threshold' | 'grace-days' | 'escalate-after' | 'review-sla' | 'pending-sla' | 'entry-date' | 'pot' | 'alert-threshold' | 'parent' | 'fiscal-start-month' | 'rounding' | 'proof-kinds' | 'default-grade' | 'score-mid' | 'score-high' | 'score-log' | 'max-open' | PermKey;

const GRADES = ['A', 'B', 'C'] as const;

/** The settings that carry a figure: each is saved from a from-date (blank for the sheet's today). */
type DatedKey = Exclude<Key, 'policy-date' | 'period-numbering-from' | 'grace-days' | 'escalate-after' | 'review-sla' | 'pending-sla' | 'entry-date' | 'pot' | 'alert-threshold' | 'parent' | 'fiscal-start-month' | 'proof-kinds' | 'default-grade' | 'score-mid' | 'score-high' | 'score-log' | 'max-open' | PermKey>;

const NO_FROM: Record<DatedKey, string> = { 'clerk-limit': '', 'clerk-limit-A': '', 'clerk-limit-B': '', 'clerk-limit-C': '', floor: '', terms: '', 'terms-A': '', 'terms-B': '', 'terms-C': '', 'grace-A': '', 'grace-B': '', 'grace-C': '', 'late-pct': '', cap: '', 'proof-threshold': '', 'proof-threshold-A': '', 'proof-threshold-B': '', 'proof-threshold-C': '', 'note-threshold': '', 'review-threshold': '', rounding: '' };

const isDated = (k: Key): k is DatedKey => k in NO_FROM;

// The sheet's settings in force, and the form that changes them: the clerk limit (blank for none), each grade's
// own clerk limit, which a claim of a person of the grade on the sheet's today takes instead (blank for none: the sheet's),
// the floor (blank for its default), the terms in days (blank for 14), each grade's own terms (blank
// for the sheet's terms), each grade's grace days (blank for none: whole days after its due-by a claim of
// a person of the grade on its date is not yet overdue, and its late charge counts from) and the late percent (blank for
// 0.00), the policy date rounding turns on (blank for the sheet's today) and the date claims opened
// are numbered per period from (blank for off), and the cap on a person's due-total (blank for none), and the proof threshold above which a claim's due needs a proof to be settled or approved (blank for none), and each grade's own proof threshold, which a claim of a person of the grade on its date takes instead (blank for none: the sheet's), and the note threshold above which a claim's due needs a note to be opened (blank for none), and the review threshold above which a claim's due sends it to REVIEW when opened (blank for none). Saving changes the setting last touched, so a field left blank does not clear the others.
// Each setting that carries a figure is saved from its from-date (blank for the sheet's today), and shows
// the value in force on the sheet's today; the policy date and period numbering date are not dated.
// The permission matrix gives, for each action, the roles that may take it, comma separated (blank keeps
// the standing rule, shown as the placeholder); it is not dated. The grace days (blank for off, not
// dated) are the whole days after its to-date an open period is closed when the sheet's today moves.
// The escalation gap (blank for off, not dated) is the whole days at least between a claim's previous reminder
// and the sheet's today for a reminder to escalate it, once per claim.
// The review SLA (blank for none, not dated) is the whole days after its review day a claim in REVIEW may stay
// there; past them it shows as review-late.
// The pending SLA (blank for none, not dated) is the whole days after its pending day (the latest day it went to
// PENDING) a claim in PENDING may stay there; past them it shows as pending-late.
// The entry date (blank for off, not dated) is the default date of a new claim in place of the sheet's today.
// The pot (blank for none, not dated) is shared out among each balanced pool's members by their shares.
// The alert threshold (blank for off, not dated) is the due-total above which a person a change takes there
// raises an ALERT in the audit file.
// The parent (a sheet id, blank for none, not dated) is the sheet whose rates a grade with no rate in force on a
// date takes, then its parent's, up to three levels up.
// The rounding mode (half-up or half-even, blank for none) is saved from its from-date; a claim whose date has
// one in force rounds its live figures by it, else by the policy date.
// The default grade (A, B or C, blank for A, not dated) is the grade the quick-add bar and an import line with a
// blank grade give a new person.
// The fiscal start month (1 to 12, not dated) is the month a fiscal year starts in; 1 unless set.
// The proof kinds (receipt, photo; comma separated, blank for none, not dated) are the kinds a claim above its proof
// threshold needs at least one proof of each of before it is settled or approved.
// The score log (on, blank for off, not dated) writes each person's score on the day being left to the audit file
// when the sheet's today moves.
// The maximum of open claims (a whole number, blank for none, not dated) is the most OPEN or PENDING claims a person
// may have for another of theirs to be opened.
function SheetSettings() {
  const fig = useFigure();
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const settings = useSettings(sheet);
  const [values, setValues] = useState<Record<Key, string>>({ 'clerk-limit': '', 'clerk-limit-A': '', 'clerk-limit-B': '', 'clerk-limit-C': '', floor: '', terms: '', 'terms-A': '', 'terms-B': '', 'terms-C': '', 'grace-A': '', 'grace-B': '', 'grace-C': '', 'late-pct': '', 'policy-date': '', 'period-numbering-from': '', cap: '', 'proof-threshold': '', 'proof-threshold-A': '', 'proof-threshold-B': '', 'proof-threshold-C': '', 'note-threshold': '', 'review-threshold': '', 'grace-days': '', 'escalate-after': '', 'review-sla': '', 'pending-sla': '', 'entry-date': '', pot: '', 'alert-threshold': '', parent: '', 'fiscal-start-month': '', rounding: '', 'proof-kinds': '', 'default-grade': '', 'score-mid': '', 'score-high': '', 'score-log': '', 'max-open': '',
    'perm-open': '', 'perm-settle': '', 'perm-void': '', 'perm-reassign': '', 'perm-close': '', 'perm-reopen': '', 'perm-today': '' });
  const [froms, setFroms] = useState<Record<DatedKey, string>>(NO_FROM);
  const [key, setKey] = useState<Key>('clerk-limit');
  const queryClient = useQueryClient();

  const save = useMutation({
    mutationFn: ({ key, value, from }: { key: Key; value: string; from: string }) =>
      postJson<Settings>(`/api/sheets/${encodeURIComponent(sheet)}/settings`, { key, value, from, user }),
    onSuccess: async (_, { key }) => {
      await queryClient.invalidateQueries({ queryKey: settingsKey(sheet) });
      if (key === 'pot') await queryClient.invalidateQueries({ queryKey: poolsKey(sheet) });
      if (key === 'parent') await queryClient.invalidateQueries({ queryKey: ratesKey(sheet) });
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
      <p>
        Policy version <strong data-testid="policy-version">{settings.data?.policyVersion ?? ''}</strong>
      </p>
      <form
        data-testid="settings-form"
        onSubmit={(e) => {
          e.preventDefault();
          submit({ key, value: values[key], from: isDated(key) ? froms[key] : '' });
        }}
      >
        Clerk limit <strong data-testid="setting-clerk-limit-current">{fig(settings.data?.clerkLimit ?? '')}</strong>{' '}
        {field('clerk-limit', 'Clerk limit (blank for none)')}{' '}{from('clerk-limit')}{' '}
        {GRADES.map((g) => (
          <span key={g}>
            Clerk limit {g} <strong data-testid={`setting-clerk-limit-${g}-current`}>{fig(settings.data?.gradeClerkLimit?.[g] ?? '')}</strong>{' '}
            {field(`clerk-limit-${g}`, `Clerk limit for grade ${g} (blank for the sheet's)`)}{' '}{from(`clerk-limit-${g}`)}{' '}
          </span>
        ))}
        Floor <strong data-testid="setting-floor-current">{fig(settings.data?.floor ?? '')}</strong>{' '}
        {field('floor', 'Floor (blank for 1.00)')}{' '}{from('floor')}{' '}
        Terms <strong data-testid="setting-terms-current">{settings.data?.terms ?? ''}</strong>{' '}
        {field('terms', 'Terms in days (blank for 14)')}{' '}{from('terms')}{' '}
        {GRADES.map((g) => (
          <span key={g}>
            Terms {g} <strong data-testid={`setting-terms-${g}-current`}>{settings.data?.gradeTerms?.[g] ?? ''}</strong>{' '}
            {field(`terms-${g}`, `Terms for grade ${g} (blank for the sheet's)`)}{' '}{from(`terms-${g}`)}{' '}
          </span>
        ))}
        {GRADES.map((g) => (
          <span key={g}>
            Grace {g} <strong data-testid={`setting-grace-${g}-current`}>{settings.data?.gradeGrace?.[g] ?? ''}</strong>{' '}
            {field(`grace-${g}`, `Grace days for grade ${g} (blank for none)`)}{' '}{from(`grace-${g}`)}{' '}
          </span>
        ))}
        Late percent <strong data-testid="setting-late-pct-current">{fig(settings.data?.latePct ?? '')}</strong>{' '}
        {field('late-pct', 'Late percent (blank for 0.00)')}{' '}{from('late-pct')}{' '}
        Policy date <strong data-testid="setting-policy-date-current">{settings.data?.policyDate ?? ''}</strong>{' '}
        {field('policy-date', 'Policy date YYYY-MM-DD (blank for today)', 'text')}{' '}
        Rounding <strong data-testid="setting-rounding-current">{fig(settings.data?.rounding ?? '')}</strong>{' '}
        {field('rounding', 'Rounding: half-up or half-even (blank for none)', 'text')}{' '}{from('rounding')}{' '}
        Period numbering from <strong data-testid="setting-period-numbering-from-current">{settings.data?.periodNumberingFrom ?? ''}</strong>{' '}
        {field('period-numbering-from', 'Period numbering from YYYY-MM-DD (blank for off)', 'text')}{' '}
        Cap <strong data-testid="setting-cap-current">{fig(settings.data?.cap ?? '')}</strong>{' '}
        {field('cap', 'Cap (blank for none)')}{' '}{from('cap')}{' '}
        Proof threshold <strong data-testid="setting-proof-threshold-current">{fig(settings.data?.proofThreshold ?? '')}</strong>{' '}
        {field('proof-threshold', 'Proof threshold (blank for none)')}{' '}{from('proof-threshold')}{' '}
        {GRADES.map((g) => (
          <span key={g}>
            Proof threshold {g} <strong data-testid={`setting-proof-threshold-${g}-current`}>{fig(settings.data?.gradeProofThreshold?.[g] ?? '')}</strong>{' '}
            {field(`proof-threshold-${g}`, `Proof threshold for grade ${g} (blank for the sheet's)`)}{' '}{from(`proof-threshold-${g}`)}{' '}
          </span>
        ))}
        Proof kinds <strong data-testid="setting-proof-kinds-current">{settings.data?.proofKinds ?? ''}</strong>{' '}
        {field('proof-kinds', 'Proof kinds: receipt, photo, comma separated (blank for none)', 'text')}{' '}
        Note threshold <strong data-testid="setting-note-threshold-current">{fig(settings.data?.noteThreshold ?? '')}</strong>{' '}
        {field('note-threshold', 'Note threshold (blank for none)')}{' '}{from('note-threshold')}{' '}
        Review threshold <strong data-testid="setting-review-threshold-current">{fig(settings.data?.reviewThreshold ?? '')}</strong>{' '}
        {field('review-threshold', 'Review threshold (blank for none)')}{' '}{from('review-threshold')}{' '}
        Grace days <strong data-testid="setting-grace-days-current">{settings.data?.graceDays ?? ''}</strong>{' '}
        {field('grace-days', 'Grace days (blank for off)')}{' '}
        Escalate after <strong data-testid="setting-escalate-after-current">{settings.data?.escalateAfter ?? ''}</strong>{' '}
        {field('escalate-after', 'Escalate after days (blank for off)')}{' '}
        Review SLA <strong data-testid="setting-review-sla-current">{settings.data?.reviewSla ?? ''}</strong>{' '}
        {field('review-sla', 'Review SLA days (blank for none)')}{' '}
        Pending SLA <strong data-testid="setting-pending-sla-current">{settings.data?.pendingSla ?? ''}</strong>{' '}
        {field('pending-sla', 'Pending SLA days (blank for none)')}{' '}
        Entry date <strong data-testid="setting-entry-date-current">{settings.data?.entryDate ?? ''}</strong>{' '}
        {field('entry-date', 'Entry date YYYY-MM-DD (blank for off)', 'text')}{' '}
        Score mid <strong data-testid="setting-score-mid-current">{settings.data?.scoreMid ?? ''}</strong>{' '}
        {field('score-mid', 'Score for MID band (blank for none)')}{' '}
        Score high <strong data-testid="setting-score-high-current">{settings.data?.scoreHigh ?? ''}</strong>{' '}
        {field('score-high', 'Score for HIGH band (blank for none)')}{' '}
        Score log <strong data-testid="setting-score-log-current">{settings.data?.scoreLog ?? ''}</strong>{' '}
        {field('score-log', 'Score log: on (blank for off)', 'text')}{' '}
        Max open <strong data-testid="setting-max-open-current">{settings.data?.maxOpen ?? ''}</strong>{' '}
        {field('max-open', 'Maximum open claims per person (blank for none)')}{' '}
        Pot <strong data-testid="setting-pot-current">{fig(settings.data?.pot ?? '')}</strong>{' '}
        {field('pot', 'Pot (blank for none)')}{' '}
        Alert threshold <strong data-testid="setting-alert-threshold-current">{fig(settings.data?.alertThreshold ?? '')}</strong>{' '}
        {field('alert-threshold', 'Alert threshold (blank for off)')}{' '}
        Parent <strong data-testid="setting-parent-current">{settings.data?.parent ?? ''}</strong>{' '}
        {field('parent', 'Parent sheet id (blank for none)', 'text')}{' '}
        Default grade <strong data-testid="setting-default-grade-current">{settings.data?.defaultGrade ?? ''}</strong>{' '}
        {field('default-grade', 'Default grade: A, B or C (blank for A)', 'text')}{' '}
        Fiscal start month <strong data-testid="setting-fiscal-start-month-current">{settings.data?.fiscalStartMonth ?? ''}</strong>{' '}
        {field('fiscal-start-month', 'Fiscal start month (1 to 12)')}{' '}
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
