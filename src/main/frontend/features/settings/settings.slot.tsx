import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { byUser } from '../session/session';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { poolsKey } from '../pools/pools';
import { GRADE_CLERK_LIMIT, GRADE_GRACE, GRADE_PROOF_THRESHOLD, GRADE_TERMS, PERMISSIONS, isDated, settingsKey, settingsPath, useSettings } from './settings';
import type { PermAction, SettingKey, Settings as SheetSettings } from './settings';
import { Fig } from '../locale/Fig';

// The sheet's settings: each value in force, and a form that changes it. The clerk limit is the most due
// a clerk may settle (two decimals); blank means no limit. The floor is the least a claim's live due is (two
// decimals); blank takes its default, 1.00. The terms are the whole days after its date by which a claim is due;
// blank takes its default, 14. The late percent (two decimals) is charged on an overdue claim's figure for every
// full 30 days it is overdue; blank takes its default, 0.00. A claim dated before the policy date rounds half up, on
// or after it half to even; blank makes it the sheet's today. While the sheet's today is on or after the period
// numbering from-date, a claim opened is numbered within its period; blank turns that off. A grade's own terms (whole
// days) fix the due-by of a claim added for a person of that grade; blank makes the grade take the sheet's terms. Saving sends each setting whose field was changed; with none changed it
// sends the clerk limit, so saving a blank form clears the limit. A grade's own clerk limit (two decimals, dated; blank
// means none) in force on the sheet's today takes the place of the sheet's for a claim whose person has that grade. The cap is the most a person's due-total is (two
// decimals); blank means no cap. A claim whose due is above the proof threshold in force on its date (two decimals;
// blank means none) needs a proof before it is settled or approved; a grade's own proof threshold (two decimals, dated;
// blank means none) in force on a claim's date takes the place of the sheet's for a claim whose person has that grade then. A claim whose due is above the note threshold in force on its date (two decimals;
// blank means none) cannot be opened with an empty note. A claim whose due is above the review threshold in force on its
// date (two decimals; blank means none) goes to REVIEW when opened, for a steward to review; one still in REVIEW once its review day (the sheet's today
// when it went to REVIEW) plus the review SLA (whole days, not dated; blank means none) is before the sheet's today is late for review. Each setting that carries a figure (all but the policy date and the period numbering
// from-date) has a from-date beside it: its value is recorded from that date (blank: the sheet's today), keeping the
// values recorded from other dates, and each shows the value in force on the sheet's today. The permission matrix gives,
// for each of open, settle, void, reassign, close and reopen, the roles that may take it (comma separated); blank keeps
// its standing rule (reassign and reopen by a steward, the rest by a clerk or a steward). The grace days (whole days,
// not dated; blank turns it off): a today change closes each open period whose to-date plus them is before the new today.
// The entry date (not dated; blank turns it off): while set, a new claim with a blank date takes it in place of the sheet's today.
// A grade's grace (whole days, dated; blank means none): a claim whose person's grade in force on its date has a grace
// in force on that date is overdue only once its due-by plus the grace is before the sheet's today.
// The proof kinds (receipt and photo, comma separated, not dated; blank means none): while set, a claim above the
// proof threshold needs a proof of each before it is settled or approved.
// The pot (two decimals, not dated; blank means none): what each balanced pool shares out among its members.
// The parent (a sheet id, not dated; blank means none): where the sheet has no rate in force for a grade on a date,
// the parent's is used, then its parent's, up to three levels up.
// The fiscal start month (1 to 12, not dated; 1 unless set): the month each fiscal year starts in.
// The rounding mode (half-up or half-even, dated; blank means none): a claim whose date has one in force rounds its
// live figures by it in place of the policy date.
// The default grade (A, B or C, not dated; blank means A): the grade a person added by the quick-add bar, or by an
// import line with a blank grade, takes. Changing it changes no one.
function Settings({ sheet, user }: { sheet: string; user: string }) {
  const queryClient = useQueryClient();
  const { data } = useSettings(sheet);
  const [clerkLimit, setClerkLimit] = useState('');
  const [cap, setCap] = useState('');
  const [proofThreshold, setProofThreshold] = useState('');
  const [proofKinds, setProofKinds] = useState('');
  const [noteThreshold, setNoteThreshold] = useState('');
  const [reviewThreshold, setReviewThreshold] = useState('');
  const [floor, setFloor] = useState('');
  const [terms, setTerms] = useState('');
  const [latePct, setLatePct] = useState('');
  const [policyDate, setPolicyDate] = useState('');
  const [periodNumberingFrom, setPeriodNumberingFrom] = useState('');
  const [graceDays, setGraceDays] = useState('');
  const [escalateAfter, setEscalateAfter] = useState('');
  const [reviewSla, setReviewSla] = useState('');
  const [scoreMid, setScoreMid] = useState('');
  const [scoreHigh, setScoreHigh] = useState('');
  const [scoreLog, setScoreLog] = useState('');
  const [entryDate, setEntryDate] = useState('');
  const [pot, setPot] = useState('');
  const [parent, setParent] = useState('');
  const [fiscalStartMonth, setFiscalStartMonth] = useState('');
  const [alertThreshold, setAlertThreshold] = useState('');
  const [rounding, setRounding] = useState('');
  const [defaultGrade, setDefaultGrade] = useState('');
  const [gradeTerms, setGradeTerms] = useState<Record<string, string>>({});
  const [gradeGrace, setGradeGrace] = useState<Record<string, string>>({});
  const [gradeProofThreshold, setGradeProofThreshold] = useState<Record<string, string>>({});
  const [gradeClerkLimit, setGradeClerkLimit] = useState<Record<string, string>>({});
  const [perms, setPerms] = useState<Record<string, string>>({});
  const [changed, setChanged] = useState<SettingKey[]>([]);
  const [froms, setFroms] = useState<Partial<Record<SettingKey, string>>>({});

  const save = useMutation({
    mutationFn: async (bodies: { key: SettingKey; value: string; from?: string }[]) => {
      let updated: SheetSettings | undefined;
      for (const body of bodies) {
        updated = await postJson<SheetSettings>(byUser(settingsPath(sheet), user), body);
        queryClient.setQueryData(settingsKey(sheet), updated);
      }
      return updated;
    },
    onSuccess: (_updated, bodies) => {
      // The pot moves each balanced pool's cuts.
      void queryClient.invalidateQueries({ queryKey: poolsKey(sheet) });
      // A new parent gives the sheet other rates, and every due follows them.
      if (bodies.some((b) => b.key === 'parent')) {
        void queryClient.invalidateQueries({
          predicate: (q) => q.queryKey.includes(sheet) && q.queryKey[0] !== 'sheet' && q.queryKey[0] !== 'settings',
        });
      }
      setClerkLimit('');
      setCap('');
      setProofThreshold('');
      setProofKinds('');
      setNoteThreshold('');
      setReviewThreshold('');
      setFloor('');
      setTerms('');
      setLatePct('');
      setPolicyDate('');
      setPeriodNumberingFrom('');
      setGraceDays('');
      setEscalateAfter('');
      setReviewSla('');
      setScoreMid('');
      setScoreHigh('');
      setScoreLog('');
      setEntryDate('');
      setPot('');
      setParent('');
      setFiscalStartMonth('');
      setAlertThreshold('');
      setRounding('');
      setDefaultGrade('');
      setGradeTerms({});
      setGradeGrace({});
      setGradeProofThreshold({});
      setGradeClerkLimit({});
      setPerms({});
      setFroms({});
      setChanged([]);
    },
  });
  const send = useSubmitOnce(save);

  const touch = (key: SettingKey) => {
    setChanged((keys) => (keys.includes(key) ? keys : [...keys, key]));
    save.reset();
  };

  // The from-date of a dated key's value; changing it counts as changing the key.
  const from = (key: SettingKey) => (
    <>
      <label>
        from{' '}
        <input
          data-testid={`setting-${key}-from`}
          placeholder="yyyy-mm-dd (blank: today)"
          value={froms[key] ?? ''}
          onChange={(e) => {
            const value = e.target.value;
            setFroms((all) => ({ ...all, [key]: value }));
            touch(key);
          }}
        />
      </label>{' '}
    </>
  );

  const submit = (e: FormEvent) => {
    e.preventDefault();
    // The server refuses a value that is not 0.00 or more with up to two decimals.
    const values: Record<SettingKey, string> = {
      ...(Object.fromEntries(PERMISSIONS.map(({ action }) => [`perm-${action}`, perms[action] ?? ''])) as Record<`perm-${PermAction}`, string>), 'clerk-limit': clerkLimit, floor, terms, 'late-pct': latePct, 'policy-date': policyDate,
      'period-numbering-from': periodNumberingFrom, 'terms-A': gradeTerms.A ?? '', 'terms-B': gradeTerms.B ?? '',
      'terms-C': gradeTerms.C ?? '', cap, 'proof-threshold': proofThreshold, 'proof-kinds': proofKinds, 'note-threshold': noteThreshold, 'review-threshold': reviewThreshold,
      'grace-days': graceDays, 'escalate-after': escalateAfter, 'review-sla': reviewSla, 'score-mid': scoreMid, 'score-high': scoreHigh, 'score-log': scoreLog, 'entry-date': entryDate, pot, parent, 'fiscal-start-month': fiscalStartMonth, 'alert-threshold': alertThreshold, rounding, 'default-grade': defaultGrade, 'grace-A': gradeGrace.A ?? '', 'grace-B': gradeGrace.B ?? '',
      'grace-C': gradeGrace.C ?? '', 'proof-threshold-A': gradeProofThreshold.A ?? '',
      'proof-threshold-B': gradeProofThreshold.B ?? '', 'proof-threshold-C': gradeProofThreshold.C ?? '',
      'clerk-limit-A': gradeClerkLimit.A ?? '', 'clerk-limit-B': gradeClerkLimit.B ?? '', 'clerk-limit-C': gradeClerkLimit.C ?? '' };
    const keys: SettingKey[] = changed.length === 0 ? ['clerk-limit'] : changed;
    send(keys.map((key) => (isDated(key) ? { key, value: values[key], from: froms[key] ?? '' } : { key, value: values[key] })));
  };

  return (
    <section>
      <h2>Settings</h2>
      <form onSubmit={submit}>
        <label>
          Clerk limit{' '}
          <input
            data-testid="setting-clerk-limit"
            placeholder="0.00 (blank: no limit)"
            value={clerkLimit}
            onChange={(e) => {
              setClerkLimit(e.target.value);
              touch('clerk-limit');
            }}
          />
        </label>{' '}
        {from('clerk-limit')}
        now <span data-testid="setting-clerk-limit-current"><Fig>{data?.clerkLimit ?? ''}</Fig></span>{' '}
        {GRADE_CLERK_LIMIT.map(({ grade, key, field }) => (
          <span key={key}>
            <label>
              Clerk limit for grade {grade}{' '}
              <input
                data-testid={`setting-${key}`}
                placeholder="0.00 (blank: the sheet's)"
                value={gradeClerkLimit[grade] ?? ''}
                onChange={(e) => {
                  const value = e.target.value;
                  setGradeClerkLimit((limits) => ({ ...limits, [grade]: value }));
                  touch(key);
                }}
                // As the policy date: clearing an already blank field must still send the blank.
                onFocus={() => touch(key)}
              />
            </label>{' '}
            {from(key)}
            now <span data-testid={`setting-${key}-current`}><Fig>{data?.[field] ?? ''}</Fig></span>{' '}
          </span>
        ))}
        <label>
          Cap{' '}
          <input
            data-testid="setting-cap"
            placeholder="0.00 (blank: no cap)"
            value={cap}
            onChange={(e) => {
              setCap(e.target.value);
              touch('cap');
            }}
            // As the policy date: clearing an already blank field must still send the blank.
            onFocus={() => touch('cap')}
          />
        </label>{' '}
        {from('cap')}
        now <span data-testid="setting-cap-current"><Fig>{data?.cap ?? ''}</Fig></span>{' '}
        <label>
          Proof threshold{' '}
          <input
            data-testid="setting-proof-threshold"
            placeholder="0.00 (blank: none)"
            value={proofThreshold}
            onChange={(e) => {
              setProofThreshold(e.target.value);
              touch('proof-threshold');
            }}
            // As the policy date: clearing an already blank field must still send the blank.
            onFocus={() => touch('proof-threshold')}
          />
        </label>{' '}
        {from('proof-threshold')}
        now <span data-testid="setting-proof-threshold-current"><Fig>{data?.proofThreshold ?? ''}</Fig></span>{' '}
        <label>
          Proof kinds{' '}
          <input
            data-testid="setting-proof-kinds"
            placeholder="receipt,photo (blank: none)"
            value={proofKinds}
            onChange={(e) => {
              setProofKinds(e.target.value);
              touch('proof-kinds');
            }}
            // As the policy date: clearing an already blank field must still send the blank.
            onFocus={() => touch('proof-kinds')}
          />
        </label>{' '}
        now <span data-testid="setting-proof-kinds-current">{data?.proofKinds ?? ''}</span>{' '}
        <label>
          Note threshold{' '}
          <input
            data-testid="setting-note-threshold"
            placeholder="0.00 (blank: none)"
            value={noteThreshold}
            onChange={(e) => {
              setNoteThreshold(e.target.value);
              touch('note-threshold');
            }}
            // As the policy date: clearing an already blank field must still send the blank.
            onFocus={() => touch('note-threshold')}
          />
        </label>{' '}
        {from('note-threshold')}
        now <span data-testid="setting-note-threshold-current"><Fig>{data?.noteThreshold ?? ''}</Fig></span>{' '}
        <label>
          Review threshold{' '}
          <input
            data-testid="setting-review-threshold"
            placeholder="0.00 (blank: none)"
            value={reviewThreshold}
            onChange={(e) => {
              setReviewThreshold(e.target.value);
              touch('review-threshold');
            }}
            // As the policy date: clearing an already blank field must still send the blank.
            onFocus={() => touch('review-threshold')}
          />
        </label>{' '}
        {from('review-threshold')}
        now <span data-testid="setting-review-threshold-current"><Fig>{data?.reviewThreshold ?? ''}</Fig></span>{' '}
        <label>
          Floor{' '}
          <input
            data-testid="setting-floor"
            placeholder="0.00 (blank: 1.00)"
            value={floor}
            onChange={(e) => {
              setFloor(e.target.value);
              touch('floor');
            }}
          />
        </label>{' '}
        {from('floor')}
        now <span data-testid="setting-floor-current"><Fig>{data?.floor ?? ''}</Fig></span>{' '}
        <label>
          Rounding mode{' '}
          <input
            data-testid="setting-rounding"
            placeholder="half-up or half-even (blank: none)"
            value={rounding}
            onChange={(e) => {
              setRounding(e.target.value);
              touch('rounding');
            }}
            // As the policy date: clearing an already blank field must still send the blank.
            onFocus={() => touch('rounding')}
          />
        </label>{' '}
        {from('rounding')}
        now <span data-testid="setting-rounding-current"><Fig>{data?.rounding ?? ''}</Fig></span>{' '}
        <label>
          Terms (days){' '}
          <input
            data-testid="setting-terms"
            placeholder="days (blank: 14)"
            value={terms}
            onChange={(e) => {
              setTerms(e.target.value);
              touch('terms');
            }}
          />
        </label>{' '}
        {from('terms')}
        now <span data-testid="setting-terms-current"><Fig>{data?.terms ?? ''}</Fig></span>{' '}
        {GRADE_TERMS.map(({ grade, key, field }) => (
          <span key={key}>
            <label>
              Terms for grade {grade}{' '}
              <input
                data-testid={`setting-${key}`}
                placeholder="days (blank: the sheet's terms)"
                value={gradeTerms[grade] ?? ''}
                onChange={(e) => {
                  const value = e.target.value;
                  setGradeTerms((terms) => ({ ...terms, [grade]: value }));
                  touch(key);
                }}
                // As the policy date: clearing an already blank field must still send the blank.
                onFocus={() => touch(key)}
              />
            </label>{' '}
            {from(key)}
            now <span data-testid={`setting-${key}-current`}><Fig>{data?.[field] ?? ''}</Fig></span>{' '}
          </span>
        ))}
        {GRADE_GRACE.map(({ grade, key, field }) => (
          <span key={key}>
            <label>
              Grace for grade {grade}{' '}
              <input
                data-testid={`setting-${key}`}
                placeholder="days (blank: none)"
                value={gradeGrace[grade] ?? ''}
                onChange={(e) => {
                  const value = e.target.value;
                  setGradeGrace((grace) => ({ ...grace, [grade]: value }));
                  touch(key);
                }}
                // As the policy date: clearing an already blank field must still send the blank.
                onFocus={() => touch(key)}
              />
            </label>{' '}
            {from(key)}
            now <span data-testid={`setting-${key}-current`}><Fig>{data?.[field] ?? ''}</Fig></span>{' '}
          </span>
        ))}
        {GRADE_PROOF_THRESHOLD.map(({ grade, key, field }) => (
          <span key={key}>
            <label>
              Proof threshold for grade {grade}{' '}
              <input
                data-testid={`setting-${key}`}
                placeholder="0.00 (blank: the sheet's)"
                value={gradeProofThreshold[grade] ?? ''}
                onChange={(e) => {
                  const value = e.target.value;
                  setGradeProofThreshold((thresholds) => ({ ...thresholds, [grade]: value }));
                  touch(key);
                }}
                // As the policy date: clearing an already blank field must still send the blank.
                onFocus={() => touch(key)}
              />
            </label>{' '}
            {from(key)}
            now <span data-testid={`setting-${key}-current`}><Fig>{data?.[field] ?? ''}</Fig></span>{' '}
          </span>
        ))}
        <label>
          Late percent{' '}
          <input
            data-testid="setting-late-pct"
            placeholder="0.00 (blank: 0.00)"
            value={latePct}
            onChange={(e) => {
              setLatePct(e.target.value);
              touch('late-pct');
            }}
          />
        </label>{' '}
        {from('late-pct')}
        now <span data-testid="setting-late-pct-current"><Fig>{data?.latePct ?? ''}</Fig></span>{' '}
        <label>
          Policy date{' '}
          <input
            data-testid="setting-policy-date"
            placeholder="yyyy-mm-dd (blank: today)"
            value={policyDate}
            onChange={(e) => {
              setPolicyDate(e.target.value);
              touch('policy-date');
            }}
            // Clearing an already blank field fires no change, yet blanking the policy date must still be sent:
            // taking the field up counts as changing it.
            onFocus={() => touch('policy-date')}
          />
        </label>{' '}
        now <span data-testid="setting-policy-date-current"><Fig>{data?.policyDate ?? ''}</Fig></span>{' '}
        <label>
          Period numbering from{' '}
          <input
            data-testid="setting-period-numbering-from"
            placeholder="yyyy-mm-dd (blank: off)"
            value={periodNumberingFrom}
            onChange={(e) => {
              setPeriodNumberingFrom(e.target.value);
              touch('period-numbering-from');
            }}
            // As the policy date: clearing an already blank field must still send the blank.
            onFocus={() => touch('period-numbering-from')}
          />
        </label>{' '}
        now <span data-testid="setting-period-numbering-from-current"><Fig>{data?.periodNumberingFrom ?? ''}</Fig></span>{' '}
        <label>
          Grace days{' '}
          <input
            data-testid="setting-grace-days"
            placeholder="whole days (blank: off)"
            value={graceDays}
            onChange={(e) => {
              setGraceDays(e.target.value);
              touch('grace-days');
            }}
            // As the policy date: clearing an already blank field must still send the blank.
            onFocus={() => touch('grace-days')}
          />
        </label>{' '}
        now <span data-testid="setting-grace-days-current"><Fig>{data?.graceDays ?? ''}</Fig></span>{' '}
        <label>
          Escalate after{' '}
          <input
            data-testid="setting-escalate-after"
            placeholder="whole days (blank: off)"
            value={escalateAfter}
            onChange={(e) => {
              setEscalateAfter(e.target.value);
              touch('escalate-after');
            }}
            // As the policy date: clearing an already blank field must still send the blank.
            onFocus={() => touch('escalate-after')}
          />
        </label>{' '}
        now <span data-testid="setting-escalate-after-current"><Fig>{data?.escalateAfter ?? ''}</Fig></span>{' '}
        <label>
          Review SLA{' '}
          <input
            data-testid="setting-review-sla"
            placeholder="whole days (blank: none)"
            value={reviewSla}
            onChange={(e) => {
              setReviewSla(e.target.value);
              touch('review-sla');
            }}
            // As the policy date: clearing an already blank field must still send the blank.
            onFocus={() => touch('review-sla')}
          />
        </label>{' '}
        now <span data-testid="setting-review-sla-current"><Fig>{data?.reviewSla ?? ''}</Fig></span>{' '}
        <label>
          Score mid{' '}
          <input
            data-testid="setting-score-mid"
            placeholder="whole number (blank: no bands)"
            value={scoreMid}
            onChange={(e) => {
              setScoreMid(e.target.value);
              touch('score-mid');
            }}
            // As the policy date: clearing an already blank field must still send the blank.
            onFocus={() => touch('score-mid')}
          />
        </label>{' '}
        now <span data-testid="setting-score-mid-current"><Fig>{data?.scoreMid ?? ''}</Fig></span>{' '}
        <label>
          Score high{' '}
          <input
            data-testid="setting-score-high"
            placeholder="whole number (blank: no bands)"
            value={scoreHigh}
            onChange={(e) => {
              setScoreHigh(e.target.value);
              touch('score-high');
            }}
            // As the policy date: clearing an already blank field must still send the blank.
            onFocus={() => touch('score-high')}
          />
        </label>{' '}
        now <span data-testid="setting-score-high-current"><Fig>{data?.scoreHigh ?? ''}</Fig></span>{' '}
        <label>
          Score log{' '}
          <input
            data-testid="setting-score-log"
            placeholder="on (blank: off)"
            value={scoreLog}
            onChange={(e) => {
              setScoreLog(e.target.value);
              touch('score-log');
            }}
            // As the policy date: clearing an already blank field must still send the blank.
            onFocus={() => touch('score-log')}
          />
        </label>{' '}
        now <span data-testid="setting-score-log-current">{data?.scoreLog ?? ''}</span>{' '}
        <label>
          Entry date{' '}
          <input
            data-testid="setting-entry-date"
            placeholder="yyyy-mm-dd (blank: off)"
            value={entryDate}
            onChange={(e) => {
              setEntryDate(e.target.value);
              touch('entry-date');
            }}
            // As the policy date: clearing an already blank field must still send the blank.
            onFocus={() => touch('entry-date')}
          />
        </label>{' '}
        now <span data-testid="setting-entry-date-current"><Fig>{data?.entryDate ?? ''}</Fig></span>{' '}
        <label>
          Pot{' '}
          <input
            data-testid="setting-pot"
            placeholder="0.00 (blank: none)"
            value={pot}
            onChange={(e) => {
              setPot(e.target.value);
              touch('pot');
            }}
            // As the policy date: clearing an already blank field must still send the blank.
            onFocus={() => touch('pot')}
          />
        </label>{' '}
        now <span data-testid="setting-pot-current"><Fig>{data?.pot ?? ''}</Fig></span>{' '}
        <label>
          Parent sheet{' '}
          <input
            data-testid="setting-parent"
            placeholder="sheet id (blank: none)"
            value={parent}
            onChange={(e) => {
              setParent(e.target.value);
              touch('parent');
            }}
            // As the policy date: clearing an already blank field must still send the blank.
            onFocus={() => touch('parent')}
          />
        </label>{' '}
        now <span data-testid="setting-parent-current"><Fig>{data?.parent ?? ''}</Fig></span>{' '}
        <label>
          Fiscal year starts in month{' '}
          <input
            data-testid="setting-fiscal-start-month"
            placeholder="1 to 12"
            value={fiscalStartMonth}
            onChange={(e) => {
              setFiscalStartMonth(e.target.value);
              touch('fiscal-start-month');
            }}
            onFocus={() => touch('fiscal-start-month')}
          />
        </label>{' '}
        now <span data-testid="setting-fiscal-start-month-current"><Fig>{data?.fiscalStartMonth ?? ''}</Fig></span>{' '}
        <label>
          Alert threshold{' '}
          <input
            data-testid="setting-alert-threshold"
            placeholder="0.00 (blank: off)"
            value={alertThreshold}
            onChange={(e) => {
              setAlertThreshold(e.target.value);
              touch('alert-threshold');
            }}
            // As the policy date: clearing an already blank field must still send the blank.
            onFocus={() => touch('alert-threshold')}
          />
        </label>{' '}
        now <span data-testid="setting-alert-threshold-current"><Fig>{data?.alertThreshold ?? ''}</Fig></span>{' '}
        <label>
          Default grade{' '}
          <input
            data-testid="setting-default-grade"
            placeholder="A, B or C (blank: A)"
            value={defaultGrade}
            onChange={(e) => {
              setDefaultGrade(e.target.value);
              touch('default-grade');
            }}
            // As the policy date: clearing an already blank field must still send the blank.
            onFocus={() => touch('default-grade')}
          />
        </label>{' '}
        now <span data-testid="setting-default-grade-current">{data?.defaultGrade ?? ''}</span>{' '}
        {PERMISSIONS.map(({ action, standing }) => (
          <span key={action}>
            <label>
              Roles that may {action}{' '}
              <input
                data-testid={`setting-perm-${action}`}
                placeholder={`clerk,steward (blank: ${standing})`}
                value={perms[action] ?? ''}
                onChange={(e) => {
                  const value = e.target.value;
                  setPerms((all) => ({ ...all, [action]: value }));
                  touch(`perm-${action}`);
                }}
                // As the policy date: clearing an already blank field must still send the blank.
                onFocus={() => touch(`perm-${action}`)}
              />
            </label>{' '}
            now <span data-testid={`setting-perm-${action}-current`}><Fig>{data?.perms?.[action] ?? ''}</Fig></span>{' '}
          </span>
        ))}
        <button type="submit" data-testid="settings-save" disabled={save.isPending}>
          Save settings
        </button>
        {save.isError && (
          <span data-testid="settings-error" role="alert">
            {' '}The clerk limit, the cap, the proof threshold (the sheet's and each grade's), the note threshold, the review threshold, the floor, the late percent, the pot and the alert threshold must be 0.00 or more with up to two decimals, the terms (the sheet's and each grade's) and each grade's grace and the review SLA a whole number of days, the policy date, the period numbering from-date, the entry date and each from-date dates, each action's roles clerk or steward (comma separated), or blank; the parent a sheet that has been loaded, not this sheet nor one that has it as an ancestor, or blank; the fiscal start month a month, 1 to 12; the rounding mode half-up or half-even, or blank; the proof kinds receipt or photo, comma separated, or blank; the default grade A, B or C, or blank.
          </span>
        )}
      </form>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 6, Component: Settings });
