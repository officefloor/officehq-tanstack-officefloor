import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSingleFlight } from '../../query/useSingleFlight';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useSheetToday } from '../today/useSheetToday';
import { claimsKey } from '../claims/useClaims';
import { peopleKey } from '../people/usePeople';
import { poolsKey } from '../pools/usePools';
import { ratesKey } from '../rates/useRates';
import { settingOn, settingsKey, useSettings, type Settings } from './useSettings';
import { useFigure } from '../locale/useLocale';

/** The sheet's settings: its clerk limit (a clerk settles a claim only when its due is at or below
 *  it; blank is no limit), its floor (the least a live due can be), its terms (the days after
 *  a claim's date that it is due by), its late percent (what an overdue claim's due gains, of its
 *  figure, for every full 30 days it is overdue), its policy date (a claim dated before it rounds half
 *  up, one on or after it half to even; blank is the sheet's today), its period numbering from-date
 *  (while set and the sheet's today is on or after it, a claim opened is numbered within its period,
 *  <period>-<n>, or X-<n> in no period; blank is off), each grade's own terms (blank is the sheet's
 *  terms), its cap (the most a person's due-total can be; blank is none), its proof threshold (a claim whose due is
 *  above the one in force on its date cannot be settled or approved without a proof; blank is none), its note threshold (a claim
 *  whose due is above the one in force on its date cannot be opened with an empty note; blank is none), its rounding mode
 *  (half-up or half-even: a claim whose date has one in force rounds its live figures by it instead of by the policy date;
 *  blank is none), its permission matrix (for each of open, settle, void,
 *  reassign, close and reopen, the roles that may do it, comma separated; blank keeps the standing rule; not dated), its grace days (changing the
 *  sheet's today closes each open period whose to-date plus them is before the new today; blank is off; not dated), its escalation gap
 *  (reminding a claim whose previous reminder was at least that many days before the sheet's today escalates it, once per
 *  claim ever; blank is off; not dated), its review SLA (days a REVIEW claim may wait from its review day before it is
 *  review-late; blank is none; not dated), its entry date (while set, the default date of a new claim — the claim form, the
 *  quick-add bar, a pasted line with a blank date and a correction — instead of the sheet's today; blank is off; not dated), its pot (two
 *  decimals, shared out as the cuts of each balanced pool; blank is none; not dated), its parent sheet (a grade with no
 *  rate in force on a date takes the parent's, then the parent's parent's, up to three levels up; a sheet that has been
 *  loaded, not this one nor one that has this one as an ancestor; blank is none; not dated), its fiscal year start month (1 to 12; not dated), its alert threshold (two decimals; a person whose due-total
 *  crosses above it is written an ALERT line; blank is off; not dated), and the role (clerk, steward, viewer or restricted — a viewer changes nothing; a
 *  restricted user sees on the people list only the people of their crews)
 *  assigned to each user name, and the delegations of the steward role (a steward delegates it to a user from a
 *  from-date, blank the sheet's today, to a to-date, inclusive; while the sheet's today is inside it the user acts
 *  as a steward), and the sheet's holidays (a date already on the list is refused). Saving saves each setting whose field was entered (focused or typed
 *  in, even when left blank); the others stay as they are. A setting with a figure (the clerk limit, the
 *  floor, the terms, each grade's terms, the late percent, the cap, the proof threshold, the note threshold and the rounding mode) is saved from its from-date, blank
 *  meaning the sheet's today; each shows the value in force on the sheet's today. */
/** The grades a person can have, each of which can have its own terms. */
const GRADES = ['A', 'B', 'C'];

/** The actions the permission matrix covers. */
const PERM_ACTIONS = ['open', 'settle', 'void', 'reassign', 'close', 'reopen'];

/** The from-date field of a setting with a figure: blank is the sheet's today. */
function FromInput({ settingKey, from, setFrom, onChange }: { settingKey: string; from: Record<string, string>;
  setFrom: (f: (v: Record<string, string>) => Record<string, string>) => void; onChange: () => void }) {
  return (
    <label>
      from{' '}
      <input
        data-testid={`setting-${settingKey}-from`}
        placeholder="yyyy-mm-dd"
        value={from[settingKey] ?? ''}
        onChange={(e) => {
          const value = e.target.value;
          setFrom((v) => ({ ...v, [settingKey]: value }));
          onChange();
        }}
      />
    </label>
  );
}

function SettingsPanel({ sheetId }: { sheetId: string }) {
  const fig = useFigure();
  const settings = useSettings(sheetId);
  const queryClient = useQueryClient();
  const [limit, setLimit] = useState<string | null>(null);
  const [floor, setFloor] = useState<string | null>(null);
  const [terms, setTerms] = useState<string | null>(null);
  const [gradeTerms, setGradeTerms] = useState<Record<string, string>>({});
  const [gradeGrace, setGradeGrace] = useState<Record<string, string>>({});
  const [latePct, setLatePct] = useState<string | null>(null);
  const [policyDate, setPolicyDate] = useState<string | null>(null);
  const [numberingFrom, setNumberingFrom] = useState<string | null>(null);
  const [cap, setCap] = useState<string | null>(null);
  const [proofThreshold, setProofThreshold] = useState<string | null>(null);
  const [noteThreshold, setNoteThreshold] = useState<string | null>(null);
  const [reviewThreshold, setReviewThreshold] = useState<string | null>(null);
  const [gradeProof, setGradeProof] = useState<Record<string, string>>({});
  const [gradeLimit, setGradeLimit] = useState<Record<string, string>>({});
  const [from, setFrom] = useState<Record<string, string>>({});
  const [permissions, setPermissions] = useState<Record<string, string>>({});
  const [graceDays, setGraceDays] = useState<string | null>(null);
  const [escalateAfter, setEscalateAfter] = useState<string | null>(null);
  const [reviewSla, setReviewSla] = useState<string | null>(null);
  const [proofKinds, setProofKinds] = useState<string | null>(null);
  const [defaultGrade, setDefaultGrade] = useState<string | null>(null);
  const [entryDate, setEntryDate] = useState<string | null>(null);
  const [pot, setPot] = useState<string | null>(null);
  const [parent, setParent] = useState<string | null>(null);
  const [fiscalStartMonth, setFiscalStartMonth] = useState<string | null>(null);
  const [alertThreshold, setAlertThreshold] = useState<string | null>(null);
  const [rounding, setRounding] = useState<string | null>(null);
  const today = useSheetToday(sheetId).data?.today;
  const [user, setUser] = useState('');
  const [role, setRole] = useState('');

  const [delegateUser, setDelegateUser] = useState('');
  const [delegateFrom, setDelegateFrom] = useState('');
  const [delegateTo, setDelegateTo] = useState('');

  const [holidayDate, setHolidayDate] = useState('');
  const [voidReason, setVoidReason] = useState('');

  const saveOnce = useSingleFlight();
  const save = useMutation({
    mutationFn: (f: { clerkLimit: string | null; floor: string | null; terms: string | null; latePct: string | null; policyDate: string | null; periodNumberingFrom: string | null; gradeTerms: Record<string, string>; cap: string | null; from: Record<string, string>; proofThreshold: string | null; noteThreshold: string | null; permissions: Record<string, string>; graceDays: string | null; entryDate: string | null; gradeGrace: Record<string, string>; gradeProofThreshold: Record<string, string>; pot: string | null; parent: string | null; fiscalStartMonth: string | null; alertThreshold: string | null; rounding: string | null; reviewThreshold: string | null; escalateAfter: string | null; reviewSla: string | null; proofKinds: string | null; defaultGrade: string | null; gradeClerkLimit: Record<string, string> }) =>
      postJson<Settings>(`/api/sheets/${encodeURIComponent(sheetId)}/settings`, f),
    onSuccess: (saved) => {
      setLimit(null);
      setFloor(null);
      setTerms(null);
      setGradeTerms({});
      setGradeGrace({});
      setGradeProof({});
      setGradeLimit({});
      setLatePct(null);
      setPolicyDate(null);
      setNumberingFrom(null);
      setCap(null);
      setProofThreshold(null);
      setNoteThreshold(null);
      setReviewThreshold(null);
      setFrom({});
      setPermissions({});
      setGraceDays(null);
      setEscalateAfter(null);
      setReviewSla(null);
      setProofKinds(null);
      setDefaultGrade(null);
      setEntryDate(null);
      setPot(null);
      setParent(null);
      setFiscalStartMonth(null);
      setAlertThreshold(null);
      setRounding(null);
      queryClient.setQueryData(settingsKey(sheetId), saved);
      // The pot is shared out as the cuts of the balanced pools.
      void queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) });
      // A grade with no rate of its own falls back to the parent sheets' rates.
      void queryClient.invalidateQueries({ queryKey: ratesKey(sheetId) });
      // The rounding mode changes how the claims' live figures round.
      void queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
      void queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
    },
  });

  const roleOnce = useSingleFlight();
  const assign = useMutation({
    mutationFn: (f: { user: string; role: string }) =>
      postJson<Settings>(`/api/sheets/${encodeURIComponent(sheetId)}/roles`, f),
    onSuccess: (saved) => {
      setUser('');
      setRole('');
      queryClient.setQueryData(settingsKey(sheetId), saved);
    },
  });

  const delegateOnce = useSingleFlight();
  const delegate = useMutation({
    mutationFn: (f: { user: string; from: string; to: string }) =>
      postJson<Settings>(`/api/sheets/${encodeURIComponent(sheetId)}/delegations`, f),
    onSuccess: (saved) => {
      setDelegateUser('');
      setDelegateFrom('');
      setDelegateTo('');
      queryClient.setQueryData(settingsKey(sheetId), saved);
    },
  });

  const holidayOnce = useSingleFlight();
  const addHoliday = useMutation({
    mutationFn: (f: { date: string }) =>
      postJson<Settings>(`/api/sheets/${encodeURIComponent(sheetId)}/holidays`, f),
    onSuccess: (saved) => {
      setHolidayDate('');
      queryClient.setQueryData(settingsKey(sheetId), saved);
      // A new holiday re-fixes the due-by of the claims on or after it, which can move people's figures.
      void queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
      void queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
    },
  });

  const voidReasonOnce = useSingleFlight();
  const addVoidReason = useMutation({
    mutationFn: (f: { reason: string }) =>
      postJson<Settings>(`/api/sheets/${encodeURIComponent(sheetId)}/void-reasons`, f),
    onSuccess: (saved) => {
      setVoidReason('');
      queryClient.setQueryData(settingsKey(sheetId), saved);
    },
  });

  return (
    <section data-testid="settings">
      <p>
        Parent sheet: <strong data-testid="sheet-parent">{settings.data?.parent ?? ''}</strong>
      </p>
      <form
        data-testid="settings-form"
        onSubmit={(e) => {
          e.preventDefault();
          saveOnce(() => {
            save.reset();
            return save.mutateAsync({ clerkLimit: limit, floor, terms, latePct, policyDate, periodNumberingFrom: numberingFrom, gradeTerms, cap, from, proofThreshold, noteThreshold, permissions, graceDays, entryDate, gradeGrace, gradeProofThreshold: gradeProof, pot, parent, fiscalStartMonth, alertThreshold, rounding, reviewThreshold, escalateAfter, reviewSla, proofKinds, defaultGrade, gradeClerkLimit: gradeLimit });
          });
        }}
      >
        <label>
          Clerk limit{' '}
          <input
            data-testid="setting-clerk-limit"
            inputMode="decimal"
            value={limit ?? ''}
            onFocus={() => setLimit((v) => v ?? '')}
            onChange={(e) => {
              setLimit(e.target.value);
              save.reset();
            }}
          />
        </label>{' '}
        <FromInput settingKey="clerk-limit" from={from} setFrom={setFrom} onChange={() => save.reset()} />{' '}
        {GRADES.map((g) => (
          <label key={g}>
            Clerk limit {g}{' '}
            <input
              data-testid={`setting-clerk-limit-${g}`}
              inputMode="decimal"
              value={gradeLimit[g] ?? ''}
              onFocus={() => setGradeLimit((v) => (g in v ? v : { ...v, [g]: '' }))}
              onChange={(e) => {
                setGradeLimit((v) => ({ ...v, [g]: e.target.value }));
                save.reset();
              }}
            />{' '}
            <FromInput settingKey={`clerk-limit-${g}`} from={from} setFrom={setFrom} onChange={() => save.reset()} />{' '}
          </label>
        ))}
        <label>
          Floor{' '}
          <input
            data-testid="setting-floor"
            inputMode="decimal"
            value={floor ?? ''}
            onFocus={() => setFloor((v) => v ?? '')}
            onChange={(e) => {
              setFloor(e.target.value);
              save.reset();
            }}
          />
        </label>{' '}
        <FromInput settingKey="floor" from={from} setFrom={setFrom} onChange={() => save.reset()} />{' '}
        <label>
          Terms (business days){' '}
          <input
            data-testid="setting-terms"
            inputMode="numeric"
            value={terms ?? ''}
            onFocus={() => setTerms((v) => v ?? '')}
            onChange={(e) => {
              setTerms(e.target.value);
              save.reset();
            }}
          />
        </label>{' '}
        <FromInput settingKey="terms" from={from} setFrom={setFrom} onChange={() => save.reset()} />{' '}
        {GRADES.map((g) => (
          <label key={g}>
            Terms {g} (business days){' '}
            <input
              data-testid={`setting-terms-${g}`}
              inputMode="numeric"
              value={gradeTerms[g] ?? ''}
              onFocus={() => setGradeTerms((v) => (g in v ? v : { ...v, [g]: '' }))}
              onChange={(e) => {
                setGradeTerms((v) => ({ ...v, [g]: e.target.value }));
                save.reset();
              }}
            />{' '}
            <FromInput settingKey={`terms-${g}`} from={from} setFrom={setFrom} onChange={() => save.reset()} />{' '}
          </label>
        ))}
        {GRADES.map((g) => (
          <label key={g}>
            Grace {g} (days){' '}
            <input
              data-testid={`setting-grace-${g}`}
              inputMode="numeric"
              value={gradeGrace[g] ?? ''}
              onFocus={() => setGradeGrace((v) => (g in v ? v : { ...v, [g]: '' }))}
              onChange={(e) => {
                setGradeGrace((v) => ({ ...v, [g]: e.target.value }));
                save.reset();
              }}
            />{' '}
            <FromInput settingKey={`grace-${g}`} from={from} setFrom={setFrom} onChange={() => save.reset()} />{' '}
          </label>
        ))}
        <label>
          Late %{' '}
          <input
            data-testid="setting-late-pct"
            inputMode="decimal"
            value={latePct ?? ''}
            onFocus={() => setLatePct((v) => v ?? '')}
            onChange={(e) => {
              setLatePct(e.target.value);
              save.reset();
            }}
          />
        </label>{' '}
        <FromInput settingKey="late-pct" from={from} setFrom={setFrom} onChange={() => save.reset()} />{' '}
        <label>
          Policy date{' '}
          <input
            data-testid="setting-policy-date"
            placeholder="yyyy-mm-dd"
            value={policyDate ?? ''}
            onFocus={() => setPolicyDate((v) => v ?? '')}
            onChange={(e) => {
              setPolicyDate(e.target.value);
              save.reset();
            }}
          />
        </label>{' '}
        <label>
          Period numbering from{' '}
          <input
            data-testid="setting-period-numbering-from"
            placeholder="yyyy-mm-dd"
            value={numberingFrom ?? ''}
            onFocus={() => setNumberingFrom((v) => v ?? '')}
            onChange={(e) => {
              setNumberingFrom(e.target.value);
              save.reset();
            }}
          />
        </label>{' '}
        <label>
          Cap{' '}
          <input
            data-testid="setting-cap"
            inputMode="decimal"
            value={cap ?? ''}
            onFocus={() => setCap((v) => v ?? '')}
            onChange={(e) => {
              setCap(e.target.value);
              save.reset();
            }}
          />
        </label>{' '}
        <FromInput settingKey="cap" from={from} setFrom={setFrom} onChange={() => save.reset()} />{' '}
        <label>
          Proof threshold{' '}
          <input
            data-testid="setting-proof-threshold"
            inputMode="decimal"
            value={proofThreshold ?? ''}
            onFocus={() => setProofThreshold((v) => v ?? '')}
            onChange={(e) => {
              setProofThreshold(e.target.value);
              save.reset();
            }}
          />
        </label>{' '}
        <FromInput settingKey="proof-threshold" from={from} setFrom={setFrom} onChange={() => save.reset()} />{' '}
        {GRADES.map((g) => (
          <label key={g}>
            Proof threshold {g}{' '}
            <input
              data-testid={`setting-proof-threshold-${g}`}
              inputMode="decimal"
              value={gradeProof[g] ?? ''}
              onFocus={() => setGradeProof((v) => (g in v ? v : { ...v, [g]: '' }))}
              onChange={(e) => {
                setGradeProof((v) => ({ ...v, [g]: e.target.value }));
                save.reset();
              }}
            />{' '}
            <FromInput settingKey={`proof-threshold-${g}`} from={from} setFrom={setFrom} onChange={() => save.reset()} />{' '}
          </label>
        ))}
        <label>
          Note threshold{' '}
          <input
            data-testid="setting-note-threshold"
            inputMode="decimal"
            value={noteThreshold ?? ''}
            onFocus={() => setNoteThreshold((v) => v ?? '')}
            onChange={(e) => {
              setNoteThreshold(e.target.value);
              save.reset();
            }}
          />
        </label>{' '}
        <FromInput settingKey="note-threshold" from={from} setFrom={setFrom} onChange={() => save.reset()} />{' '}
        <label>
          Review threshold{' '}
          <input
            data-testid="setting-review-threshold"
            inputMode="decimal"
            value={reviewThreshold ?? ''}
            onFocus={() => setReviewThreshold((v) => v ?? '')}
            onChange={(e) => {
              setReviewThreshold(e.target.value);
              save.reset();
            }}
          />
        </label>{' '}
        <FromInput settingKey="review-threshold" from={from} setFrom={setFrom} onChange={() => save.reset()} />{' '}
        <label>
          Rounding mode{' '}
          <input
            data-testid="setting-rounding"
            placeholder="half-up or half-even"
            value={rounding ?? ''}
            onFocus={() => setRounding((v) => v ?? '')}
            onChange={(e) => {
              setRounding(e.target.value);
              save.reset();
            }}
          />
        </label>{' '}
        <FromInput settingKey="rounding" from={from} setFrom={setFrom} onChange={() => save.reset()} />{' '}
        {PERM_ACTIONS.map((a) => (
          <label key={a}>
            Who may {a}{' '}
            <input
              data-testid={`setting-perm-${a}`}
              placeholder="clerk,steward"
              value={permissions[a] ?? ''}
              onFocus={() => setPermissions((v) => (a in v ? v : { ...v, [a]: '' }))}
              onChange={(e) => {
                setPermissions((v) => ({ ...v, [a]: e.target.value }));
                save.reset();
              }}
            />{' '}
          </label>
        ))}
        <label>
          Grace days{' '}
          <input
            data-testid="setting-grace-days"
            inputMode="numeric"
            value={graceDays ?? ''}
            onFocus={() => setGraceDays((v) => v ?? '')}
            onChange={(e) => {
              setGraceDays(e.target.value);
              save.reset();
            }}
          />
        </label>{' '}
        <label>
          Escalate after (days){' '}
          <input
            data-testid="setting-escalate-after"
            inputMode="numeric"
            value={escalateAfter ?? ''}
            onFocus={() => setEscalateAfter((v) => v ?? '')}
            onChange={(e) => {
              setEscalateAfter(e.target.value);
              save.reset();
            }}
          />
        </label>{' '}
        <label>
          Review SLA (days){' '}
          <input
            data-testid="setting-review-sla"
            inputMode="numeric"
            value={reviewSla ?? ''}
            onFocus={() => setReviewSla((v) => v ?? '')}
            onChange={(e) => {
              setReviewSla(e.target.value);
              save.reset();
            }}
          />
        </label>{' '}
        <label>
          Proof kinds{' '}
          <input
            data-testid="setting-proof-kinds"
            placeholder="receipt,photo"
            value={proofKinds ?? ''}
            onFocus={() => setProofKinds((v) => v ?? '')}
            onChange={(e) => {
              setProofKinds(e.target.value);
              save.reset();
            }}
          />
        </label>{' '}
        <label>
          Default grade{' '}
          <input
            data-testid="setting-default-grade"
            placeholder="A"
            value={defaultGrade ?? ''}
            onFocus={() => setDefaultGrade((v) => v ?? '')}
            onChange={(e) => {
              setDefaultGrade(e.target.value);
              save.reset();
            }}
          />
        </label>{' '}
        <label>
          Entry date{' '}
          <input
            data-testid="setting-entry-date"
            placeholder="yyyy-mm-dd"
            value={entryDate ?? ''}
            onFocus={() => setEntryDate((v) => v ?? '')}
            onChange={(e) => {
              setEntryDate(e.target.value);
              save.reset();
            }}
          />
        </label>{' '}
        <label>
          Pot{' '}
          <input
            data-testid="setting-pot"
            inputMode="decimal"
            value={pot ?? ''}
            onFocus={() => setPot((v) => v ?? '')}
            onChange={(e) => {
              setPot(e.target.value);
              save.reset();
            }}
          />
        </label>{' '}
        <label>
          Parent sheet{' '}
          <input
            data-testid="setting-parent"
            value={parent ?? ''}
            onFocus={() => setParent((v) => v ?? '')}
            onChange={(e) => {
              setParent(e.target.value);
              save.reset();
            }}
          />
        </label>{' '}
        <label>
          Fiscal year starts in month{' '}
          <input
            data-testid="setting-fiscal-start-month"
            inputMode="numeric"
            value={fiscalStartMonth ?? ''}
            onFocus={() => setFiscalStartMonth((v) => v ?? '')}
            onChange={(e) => {
              setFiscalStartMonth(e.target.value);
              save.reset();
            }}
          />
        </label>{' '}
        <label>
          Alert threshold{' '}
          <input
            data-testid="setting-alert-threshold"
            inputMode="decimal"
            value={alertThreshold ?? ''}
            onFocus={() => setAlertThreshold((v) => v ?? '')}
            onChange={(e) => {
              setAlertThreshold(e.target.value);
              save.reset();
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="settings-save" disabled={save.isPending}>
          Save settings
        </button>{' '}
        Current: clerk limit <strong data-testid="setting-clerk-limit-current">{fig(settingOn(settings.data, 'clerk-limit', today) ?? '')}</strong>,
        {GRADES.map((g) => (
          <span key={g}>
            clerk limit {g} <strong data-testid={`setting-clerk-limit-${g}-current`}>{fig(settingOn(settings.data, `clerk-limit-${g}`, today) ?? '')}</strong>,{' '}
          </span>
        ))}
        floor <strong data-testid="setting-floor-current">{fig(settingOn(settings.data, 'floor', today) ?? '')}</strong>,
        terms <strong data-testid="setting-terms-current">{fig(settingOn(settings.data, 'terms', today) ?? '')}</strong>,
        {GRADES.map((g) => (
          <span key={g}>
            terms {g} <strong data-testid={`setting-terms-${g}-current`}>{fig(settingOn(settings.data, `terms-${g}`, today) ?? '')}</strong>,{' '}
          </span>
        ))}
        {GRADES.map((g) => (
          <span key={g}>
            grace {g} <strong data-testid={`setting-grace-${g}-current`}>{fig(settingOn(settings.data, `grace-${g}`, today) ?? '')}</strong>,{' '}
          </span>
        ))}
        late % <strong data-testid="setting-late-pct-current">{fig(settingOn(settings.data, 'late-pct', today) ?? '')}</strong>,
        policy date <strong data-testid="setting-policy-date-current">{fig(settings.data?.policyDate ?? '')}</strong>,
        period numbering from{' '}
        <strong data-testid="setting-period-numbering-from-current">{fig(settings.data?.periodNumberingFrom ?? '')}</strong>,
        cap <strong data-testid="setting-cap-current">{fig(settingOn(settings.data, 'cap', today) ?? '')}</strong>,
        proof threshold <strong data-testid="setting-proof-threshold-current">{fig(settingOn(settings.data, 'proof-threshold', today) ?? '')}</strong>,
        {GRADES.map((g) => (
          <span key={g}>
            proof threshold {g} <strong data-testid={`setting-proof-threshold-${g}-current`}>{fig(settingOn(settings.data, `proof-threshold-${g}`, today) ?? '')}</strong>,{' '}
          </span>
        ))}
        note threshold <strong data-testid="setting-note-threshold-current">{fig(settingOn(settings.data, 'note-threshold', today) ?? '')}</strong>
        , review threshold <strong data-testid="setting-review-threshold-current">{fig(settingOn(settings.data, 'review-threshold', today) ?? '')}</strong>
        , rounding <strong data-testid="setting-rounding-current">{fig(settingOn(settings.data, 'rounding', today) ?? '')}</strong>
        {PERM_ACTIONS.map((a) => (
          <span key={a}>
            , may {a} <strong data-testid={`setting-perm-${a}-current`}>{fig(settings.data?.permissions?.[a] ?? '')}</strong>
          </span>
        ))}
        , grace days <strong data-testid="setting-grace-days-current">{fig(settings.data?.graceDays ?? '')}</strong>
        , escalate after <strong data-testid="setting-escalate-after-current">{fig(settings.data?.escalateAfter ?? '')}</strong>
        , review SLA <strong data-testid="setting-review-sla-current">{fig(settings.data?.reviewSla ?? '')}</strong>
        , proof kinds <strong data-testid="setting-proof-kinds-current">{fig(settings.data?.proofKinds ?? '')}</strong>
        , default grade <strong data-testid="setting-default-grade-current">{fig(settings.data?.defaultGrade ?? '')}</strong>
        , entry date <strong data-testid="setting-entry-date-current">{fig(settings.data?.entryDate ?? '')}</strong>
        , pot <strong data-testid="setting-pot-current">{fig(settings.data?.pot ?? '')}</strong>
        , parent <strong data-testid="setting-parent-current">{fig(settings.data?.parent ?? '')}</strong>
        , fiscal year starts in month{' '}
        <strong data-testid="setting-fiscal-start-month-current">{fig(settings.data?.fiscalStartMonth ?? '')}</strong>
        , alert threshold <strong data-testid="setting-alert-threshold-current">{fig(settings.data?.alertThreshold ?? '')}</strong>
        {save.isError && <p data-testid="settings-error">{save.error.message}</p>}
      </form>
      <form
        data-testid="role-form"
        onSubmit={(e) => {
          e.preventDefault();
          roleOnce(() => {
            assign.reset();
            return assign.mutateAsync({ user, role });
          });
        }}
      >
        <label>
          User{' '}
          <input
            data-testid="role-form-user"
            value={user}
            onChange={(e) => {
              setUser(e.target.value);
              assign.reset();
            }}
          />
        </label>{' '}
        <label>
          Role{' '}
          <input
            data-testid="role-form-role"
            placeholder="clerk, steward or viewer"
            value={role}
            onChange={(e) => {
              setRole(e.target.value);
              assign.reset();
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="role-form-submit" disabled={assign.isPending}>
          Set role
        </button>
        {assign.isError && <p data-testid="role-form-error">{assign.error.message}</p>}
      </form>
      <table data-testid="roles-list">
        <thead>
          <tr>
            <th>User</th>
            <th>Role</th>
          </tr>
        </thead>
        <tbody>
          {(settings.data?.roles ?? []).map((r) => (
            <tr key={r.user} data-testid={`role-row-${r.user}`}>
              <td data-testid="role-user">{r.user}</td>
              <td data-testid="role-value">{r.role}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <form
        data-testid="delegate-form"
        onSubmit={(e) => {
          e.preventDefault();
          delegateOnce(() => {
            delegate.reset();
            return delegate.mutateAsync({ user: delegateUser, from: delegateFrom, to: delegateTo });
          });
        }}
      >
        <label>
          Delegate steward to{' '}
          <input
            data-testid="delegate-user"
            value={delegateUser}
            onChange={(e) => {
              setDelegateUser(e.target.value);
              delegate.reset();
            }}
          />
        </label>{' '}
        <label>
          from{' '}
          <input
            data-testid="delegate-from"
            placeholder="yyyy-mm-dd"
            value={delegateFrom}
            onChange={(e) => {
              setDelegateFrom(e.target.value);
              delegate.reset();
            }}
          />
        </label>{' '}
        <label>
          to{' '}
          <input
            data-testid="delegate-to"
            placeholder="yyyy-mm-dd"
            value={delegateTo}
            onChange={(e) => {
              setDelegateTo(e.target.value);
              delegate.reset();
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="delegate-submit" disabled={delegate.isPending}>
          Delegate
        </button>
        {delegate.isError && <p data-testid="delegate-error">{delegate.error.message}</p>}
      </form>
      <ul data-testid="delegations-list">
        {(settings.data?.delegations ?? []).map((d, i) => (
          <li key={i} data-testid="delegate-line">
            {d.user} acts as steward for {d.by}, {d.from} to {d.to}
          </li>
        ))}
      </ul>
      <form
        data-testid="holiday-form"
        onSubmit={(e) => {
          e.preventDefault();
          holidayOnce(() => {
            addHoliday.reset();
            return addHoliday.mutateAsync({ date: holidayDate });
          });
        }}
      >
        <label>
          Holiday{' '}
          <input
            data-testid="holiday-date"
            placeholder="yyyy-mm-dd"
            value={holidayDate}
            onChange={(e) => {
              setHolidayDate(e.target.value);
              addHoliday.reset();
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="holiday-add" disabled={addHoliday.isPending}>
          Add holiday
        </button>
        {addHoliday.isError && <p data-testid="holiday-error">{addHoliday.error.message}</p>}
      </form>
      <ul data-testid="holidays-list">
        {(settings.data?.holidays ?? []).map((d) => (
          <li key={d} data-testid={`holiday-row-${d}`}>
            {d}
          </li>
        ))}
      </ul>
      <form
        data-testid="void-reason-form"
        onSubmit={(e) => {
          e.preventDefault();
          voidReasonOnce(() => {
            addVoidReason.reset();
            return addVoidReason.mutateAsync({ reason: voidReason });
          });
        }}
      >
        <label>
          Void reason{' '}
          <input
            data-testid="void-reason-text"
            value={voidReason}
            onChange={(e) => {
              setVoidReason(e.target.value);
              addVoidReason.reset();
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="void-reason-add" disabled={addVoidReason.isPending}>
          Add void reason
        </button>
        {addVoidReason.isError && <p data-testid="void-reason-error">{addVoidReason.error.message}</p>}
      </form>
      <ul data-testid="void-reasons-list">
        {(settings.data?.voidReasons ?? []).map((r) => (
          <li key={r} data-testid={`void-reason-row-${r}`}>
            {r}
          </li>
        ))}
      </ul>
      {settings.isError && <p data-testid="settings-load-error">{settings.error.message}</p>}
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 6, Component: ({ sheetId }) => <SettingsPanel sheetId={sheetId} /> });
