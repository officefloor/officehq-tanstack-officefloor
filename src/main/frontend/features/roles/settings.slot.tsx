import { Fragment, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { poolsQueryKey } from '../pools/pools';
import { ratesQueryKey } from '../rates/rates';
import { settingsPath, settingsQueryKey, useSettings, type Settings } from './roles';

type ValueKey = 'clerkLimit' | 'floor' | 'terms' | 'latePct' | 'policyDate' | 'periodNumberingFrom' | 'termsA' | 'termsB' | 'termsC' | 'cap' | 'proofThreshold' | 'graceDays' | 'noteThreshold' | 'entryDate' | 'graceA' | 'graceB' | 'graceC' | 'proofThresholdA' | 'proofThresholdB' | 'proofThresholdC' | 'pot' | 'parent' | 'fiscalStartMonth' | 'alertThreshold' | 'rounding' | 'reviewThreshold' | 'escalateAfter' | 'reviewSla' | 'proofKinds' | PermKey;

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
const DATED = ['clerkLimit', 'floor', 'terms', 'latePct', 'termsA', 'termsB', 'termsC', 'cap', 'proofThreshold', 'noteThreshold', 'graceA', 'graceB', 'graceC', 'proofThresholdA', 'proofThresholdB', 'proofThresholdC', 'rounding', 'reviewThreshold'] as const;
type DatedKey = (typeof DATED)[number];
type FromKey = `${DatedKey}From`;
type Key = ValueKey | FromKey;

/** The grades that may have their own terms, and the setting key of each. */
const GRADE_TERMS = [
  { grade: 'A', key: 'termsA' },
  { grade: 'B', key: 'termsB' },
  { grade: 'C', key: 'termsC' },
] as const;
/** The grades that may have grace days, and the setting key of each. */
const GRADE_GRACE = [
  { grade: 'A', key: 'graceA' },
  { grade: 'B', key: 'graceB' },
  { grade: 'C', key: 'graceC' },
] as const;
/** The grades that may have their own proof threshold, and the setting key of each. */
const GRADE_PROOF_THRESHOLD = [
  { grade: 'A', key: 'proofThresholdA' },
  { grade: 'B', key: 'proofThresholdB' },
  { grade: 'C', key: 'proofThresholdC' },
] as const;
type Drafts = Partial<Record<Key, string>>;

// The sheet's settings: the clerk limit in force (blank when there is none), the floor in force (a claim's live
// due is at least it), the terms in force (a claim is due by its date plus that many days), the late percent in force (an overdue
// claim's due gains, for each full 30 days overdue, the one in force on that boundary date of its figure), the policy date (a claim dated before it rounds half
// up, one on or after it half to even; blank: the sheet's today), the period numbering from (while the sheet's today is on or after it, a claim
// opened is numbered within its period: P1-1, X-1 for no period; blank: off), each grade's own terms (blank: the sheet's terms;
// a claim's due-by is fixed when it is created, from the terms of its person's grade on its date), the cap (a person's due-total is at most it; blank: none), the proof threshold (a claim whose due is above the one in force on its date cannot be settled or approved while it has no proof; blank: none), the note threshold (a claim whose due is above the one in force on its date cannot be opened while its note is empty; blank: none), the review threshold (a claim whose due is above the one in force on its date goes to REVIEW when opened, until a steward reviews it; blank: none), the escalation gap (reminding a claim whose previous reminder was at least that many days before the sheet's today escalates it, once per claim; blank: off; not dated), the review SLA (a REVIEW claim whose review day plus that many days is before the sheet's today is late; blank: none; not dated), the proof kinds (a comma separated list of receipt and photo: while set, a claim whose due is above its proof threshold needs at least one proof of each to be settled or approved; blank: none; not dated), the permission matrix (for each of open, settle, void, reassign, close and reopen, the roles that may take it; blank: the standing rule — open, settle, void and close by clerk and steward, reassign and reopen by steward; not dated), the grace days (once the sheet's today is changed, each open period whose to-date plus them is before it is closed; blank: off; not dated), the entry date (while it is set, a new claim left without a date — on the claim form, the quick-add bar, a pasted line or a correction — is dated it in place of the sheet's today; blank: off; not dated), each grade's grace days (an overdue claim's lateness — when it is overdue, and its late charge's 30-day boundaries — counts from its due-by plus those of its person's grade on its date, in force on its date; blank: none), each grade's proof threshold (in force on a claim's date, it applies to a claim of its person's grade on that date in place of the sheet's; blank: none, the sheet's applies), the pot (shared out among the members of each balanced pool by their shares; blank: none; not dated), the parent sheet (a grade with no rate in force on a date here takes the parent's, then its parent's, up to three levels up; blank: none; not dated), the fiscal year start month (1 to 12; blank: 1; not dated), the alert threshold (a person whose due-total goes above it is alerted in the audit; blank: off; not dated), the rounding mode (half-up or half-even: a claim with one in force on its date rounds every live figure by it; blank: none, the policy date decides), and the form that sets them. A clerk settles a claim only when its due is at or below the
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
      await queryClient.invalidateQueries({ queryKey: poolsQueryKey(sheet ?? '') });
      await queryClient.invalidateQueries({ queryKey: ratesQueryKey(sheet ?? '') });
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
    graceDays: settings.data?.graceDays ?? '',
    noteThreshold: settings.data?.noteThreshold ?? '',
    entryDate: settings.data?.entryDate ?? '',
    graceA: settings.data?.graceA ?? '',
    graceB: settings.data?.graceB ?? '',
    graceC: settings.data?.graceC ?? '',
    proofThresholdA: settings.data?.proofThresholdA ?? '',
    proofThresholdB: settings.data?.proofThresholdB ?? '',
    proofThresholdC: settings.data?.proofThresholdC ?? '',
    pot: settings.data?.pot ?? '',
    parent: settings.data?.parent ?? '',
    fiscalStartMonth: settings.data?.fiscalStartMonth ?? '',
    alertThreshold: settings.data?.alertThreshold ?? '',
    rounding: settings.data?.rounding ?? '',
    reviewThreshold: settings.data?.reviewThreshold ?? '',
    escalateAfter: settings.data?.escalateAfter ?? '',
    reviewSla: settings.data?.reviewSla ?? '',
    proofKinds: settings.data?.proofKinds ?? '',
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
      <p>
        Entry date (blank: off): <span data-testid="setting-entry-date-current">{settings.data?.entryDate ?? ''}</span>
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
      {GRADE_PROOF_THRESHOLD.map(({ grade, key }) => (
        <p key={grade}>
          Proof threshold of grade {grade} (blank: the sheet's):{' '}
          <span data-testid={`setting-proof-threshold-${grade}-current`}>{settings.data?.[key] ?? ''}</span>
        </p>
      ))}
      <p>
        Note threshold:{' '}
        <span data-testid="setting-note-threshold-current">{settings.data?.noteThreshold ?? ''}</span>
      </p>
      <p>
        Review threshold:{' '}
        <span data-testid="setting-review-threshold-current">{settings.data?.reviewThreshold ?? ''}</span>
      </p>
      <p>
        Escalate after days (blank: off):{' '}
        <span data-testid="setting-escalate-after-current">{settings.data?.escalateAfter ?? ''}</span>
      </p>
      <p>
        Review SLA days (blank: none):{' '}
        <span data-testid="setting-review-sla-current">{settings.data?.reviewSla ?? ''}</span>
      </p>
      <p>
        Proof kinds (blank: none):{' '}
        <span data-testid="setting-proof-kinds-current">{settings.data?.proofKinds ?? ''}</span>
      </p>
      <p>
        Grace days (blank: off): <span data-testid="setting-grace-days-current">{settings.data?.graceDays ?? ''}</span>
      </p>
      {GRADE_GRACE.map(({ grade, key }) => (
        <p key={grade}>
          Grace days of grade {grade} (blank: none):{' '}
          <span data-testid={`setting-grace-${grade}-current`}>{settings.data?.[key] ?? ''}</span>
        </p>
      ))}
      <p>
        Pot (blank: none): <span data-testid="setting-pot-current">{settings.data?.pot ?? ''}</span>
      </p>
      <p>
        Parent sheet (blank: none): <span data-testid="setting-parent-current">{settings.data?.parent ?? ''}</span>
      </p>
      <p>
        Fiscal year start month (1 to 12; blank: 1):{' '}
        <span data-testid="setting-fiscal-start-month-current">{settings.data?.fiscalStartMonth ?? ''}</span>
      </p>
      <p>
        Alert threshold (blank: off):{' '}
        <span data-testid="setting-alert-threshold-current">{settings.data?.alertThreshold ?? ''}</span>
      </p>
      <p>
        Rounding mode (half-up or half-even; blank: by the policy date):{' '}
        <span data-testid="setting-rounding-current">{settings.data?.rounding ?? ''}</span>
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
        <input
          data-testid="setting-entry-date"
          placeholder="Entry date YYYY-MM-DD (blank: off)"
          value={valueOf('entryDate')}
          onChange={edit('entryDate')}
          onInput={edit('entryDate')}
          onFocus={touch('entryDate')}
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
        {GRADE_PROOF_THRESHOLD.map(({ grade, key }) => (
          <Fragment key={grade}>
            <input
              data-testid={`setting-proof-threshold-${grade}`}
              placeholder={`Proof threshold of grade ${grade} (blank: the sheet's)`}
              inputMode="decimal"
              value={valueOf(key)}
              onChange={edit(key)}
              onInput={edit(key)}
              onFocus={touch(key)}
            />
            {fromField(key, `setting-proof-threshold-${grade}`)}
          </Fragment>
        ))}
        <input
          data-testid="setting-note-threshold"
          placeholder="Note threshold (blank: none)"
          inputMode="decimal"
          value={valueOf('noteThreshold')}
          onChange={edit('noteThreshold')}
          onInput={edit('noteThreshold')}
          onFocus={touch('noteThreshold')}
        />
        {fromField('noteThreshold', 'setting-note-threshold')}
        <input
          data-testid="setting-review-threshold"
          placeholder="Review threshold (blank: none)"
          inputMode="decimal"
          value={valueOf('reviewThreshold')}
          onChange={edit('reviewThreshold')}
          onInput={edit('reviewThreshold')}
          onFocus={touch('reviewThreshold')}
        />
        {fromField('reviewThreshold', 'setting-review-threshold')}
        <input
          data-testid="setting-escalate-after"
          placeholder="Escalate after days (blank: off)"
          inputMode="numeric"
          value={valueOf('escalateAfter')}
          onChange={edit('escalateAfter')}
          onInput={edit('escalateAfter')}
          onFocus={touch('escalateAfter')}
        />
        <input
          data-testid="setting-review-sla"
          placeholder="Review SLA days (blank: none)"
          inputMode="numeric"
          value={valueOf('reviewSla')}
          onChange={edit('reviewSla')}
          onInput={edit('reviewSla')}
          onFocus={touch('reviewSla')}
        />
        <input
          data-testid="setting-proof-kinds"
          placeholder="Proof kinds, e.g. receipt,photo (blank: none)"
          value={valueOf('proofKinds')}
          onChange={edit('proofKinds')}
          onInput={edit('proofKinds')}
          onFocus={touch('proofKinds')}
        />
        <input
          data-testid="setting-grace-days"
          placeholder="Grace days (blank: off)"
          inputMode="numeric"
          value={valueOf('graceDays')}
          onChange={edit('graceDays')}
          onInput={edit('graceDays')}
          onFocus={touch('graceDays')}
        />
        {GRADE_GRACE.map(({ grade, key }) => (
          <Fragment key={grade}>
            <input
              data-testid={`setting-grace-${grade}`}
              placeholder={`Grace days of grade ${grade} (blank: none)`}
              inputMode="numeric"
              value={valueOf(key)}
              onChange={edit(key)}
              onInput={edit(key)}
              onFocus={touch(key)}
            />
            {fromField(key, `setting-grace-${grade}`)}
          </Fragment>
        ))}
        <input
          data-testid="setting-pot"
          placeholder="Pot (blank: none)"
          inputMode="decimal"
          value={valueOf('pot')}
          onChange={edit('pot')}
          onInput={edit('pot')}
          onFocus={touch('pot')}
        />
        <input
          data-testid="setting-parent"
          placeholder="Parent sheet id (blank: none)"
          value={valueOf('parent')}
          onChange={edit('parent')}
          onInput={edit('parent')}
          onFocus={touch('parent')}
        />
        <input
          data-testid="setting-fiscal-start-month"
          placeholder="Fiscal year start month, 1 to 12 (blank: 1)"
          inputMode="numeric"
          value={valueOf('fiscalStartMonth')}
          onChange={edit('fiscalStartMonth')}
          onInput={edit('fiscalStartMonth')}
          onFocus={touch('fiscalStartMonth')}
        />
        <input
          data-testid="setting-alert-threshold"
          placeholder="Alert threshold (blank: off)"
          inputMode="decimal"
          value={valueOf('alertThreshold')}
          onChange={edit('alertThreshold')}
          onInput={edit('alertThreshold')}
          onFocus={touch('alertThreshold')}
        />
        <input
          data-testid="setting-rounding"
          placeholder="Rounding mode: half-up or half-even (blank: none)"
          value={valueOf('rounding')}
          onChange={edit('rounding')}
          onInput={edit('rounding')}
          onFocus={touch('rounding')}
        />
        {fromField('rounding', 'setting-rounding')}
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
          <span data-testid="settings-error">The clerk limit and floor must each be an amount, 0.00 or more, with up to two decimals, or blank; the terms a whole number of days, or blank; the late percent a percent, 0.00 or more, with up to two decimals, or blank; the policy date, period numbering from and entry date each a date (YYYY-MM-DD), or blank; a grade's terms a whole number of days, or blank; the cap, proof threshold and a grade's proof threshold each an amount, 0.00 or more, with up to two decimals, or blank; the grace days, and a grade's grace days, a whole number of days, or blank; the pot an amount, 0.00 or more, with up to two decimals, or blank; the parent sheet one that has been loaded, not this sheet nor one with this sheet as an ancestor, or blank; the fiscal year start month a month, 1 to 12, or blank; the alert threshold an amount, 0.00 or more, with up to two decimals, or blank; the rounding mode half-up or half-even, or blank; a from-date a date (YYYY-MM-DD), or blank; the roles that may take an action a comma separated list of clerk and steward, or blank.</span>
        )}
      </form>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 8, Component: SheetSettings });
