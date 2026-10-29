import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { useUser } from '../session/useUser';
import { PERM_ACTIONS, ROLES, ROUNDING_MODES, rolesKey, rolesQuery, settingsKey, settingsQuery } from './settingsQuery';
import { DEFAULT_FLOOR, DEFAULT_LATE_PCT, DEFAULT_TERMS, valueOn } from './dated';
import { useSheetToday } from '../sheet/useSheetToday';
import type { PermAction, Role, RoleName, Settings as SheetSettings, SettingsChange } from './settingsQuery';
import { GRADES } from '../people/peopleQuery';
import type { Grade } from '../people/peopleQuery';
import { poolsKey } from '../pools/poolsQuery';
import { ratesKey } from '../rates/ratesQuery';

const AMOUNT = /^\d{1,9}(\.\d{1,2})?$/;
const DAYS = /^\d{1,4}$/;
const MONTH = /^\d{1,2}$/;
const DATE = /^\d{4}-\d{2}-\d{2}$/;

const PERM_ROLES = /^\s*(clerk|steward)\s*(,\s*(clerk|steward)\s*)*$/;

const isDate = (d: string) => DATE.test(d) && new Date(`${d}T00:00:00Z`).toISOString().slice(0, 10) === d;

/** The sheet's settings — the clerk limit a clerk may settle up to (blank: no limit) and the floor a
 *  claim's live due is at least, and the terms (days after its date a claim is due by) and the late percent (what an overdue claim gains per full 30 days) — and the form
 *  they are saved through. Each grade may have terms of its own (a grade without uses the sheet's terms); a claim's
 *  due-by is fixed from them when it is made. A blank floor, terms, grade's terms or late percent is left as it is, and so is a blank clerk
 *  limit saved with any of them. The policy date decides how a claim's figures round (blank: the sheet's
 *  today), unless a rounding mode (half-up or half-even, dated like the floor; blank: none) is in force on the claim's date. A person's due-total is at most the cap (blank: none). A claim whose due is above the proof threshold in force on its date (blank: none) — its person's grade's own when one is in force then (dated like the floor; blank: none, so the sheet's) — is settled or approved only once it has a proof, and one whose due is above the note threshold in force on its date (blank: none) is opened only once it has a note. From the period numbering date on, an opened claim is numbered within its period (blank: off). Each grade may have grace days (blank: none), dated like the floor: a claim whose person's grade on its date has a grace in force on that date is overdue only once its due-by plus the grace is before the sheet's today, and its late charge counts from there; a blank grade's grace is saved as none when its from-date is given or it was the last field worked in. When the sheet's today is changed, each open period whose to-date plus the grace days is before it is closed (blank: off; not dated). While the entry date is set it is the date a new claim left undated is given (blank: off, the sheet's today; not dated). Reminding a claim whose previous reminder was at least the escalation gap's days before the sheet's today escalates it, once per claim (blank: off; not dated).
 *  A save with every field blank clears whichever of the clerk limit, the policy date, the period numbering
 *  date, the cap, the proof threshold, the note threshold and the grace days and the entry date was last worked in (the clerk limit unless it was one of the others).
 *  Each setting that carries a figure (all but the policy date and the period numbering date) is saved from its from-date
 *  (blank: the sheet's today), keeping the values recorded from other dates; what is shown in force is the value on the sheet's today.
 *  The permission matrix gives, for each of open, settle, void, reassign, close and reopen, the roles allowed to do it as a comma
 *  separated list (not dated); a blank entry is the standing rule, and is saved as such when it was the last field worked in. */
function Settings({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const settings = useQuery(settingsQuery(sheetId));
  const [limit, setLimit] = useState('');
  const [floor, setFloor] = useState('');
  const [terms, setTerms] = useState('');
  const [gradeTerms, setGradeTerms] = useState<Partial<Record<Grade, string>>>({});
  const [gradeGrace, setGradeGrace] = useState<Partial<Record<Grade, string>>>({});
  const [gradeProof, setGradeProof] = useState<Partial<Record<Grade, string>>>({});
  const [latePct, setLatePct] = useState('');
  const [policyDate, setPolicyDate] = useState('');
  const [numberingFrom, setNumberingFrom] = useState('');
  const [cap, setCap] = useState('');
  const [threshold, setThreshold] = useState('');
  const [noteThreshold, setNoteThreshold] = useState('');
  const [reviewThreshold, setReviewThreshold] = useState('');
  const [rounding, setRounding] = useState('');
  const [grace, setGrace] = useState('');
  const [entryDate, setEntryDate] = useState('');
  const [pot, setPot] = useState('');
  const [alertThreshold, setAlertThreshold] = useState('');
  const [escalateAfter, setEscalateAfter] = useState('');
  const [parent, setParent] = useState('');
  const [fiscalStartMonth, setFiscalStartMonth] = useState('');
  const [perms, setPerms] = useState<Partial<Record<PermAction, string>>>({});
  const [blank, setBlank] = useState<'clerkLimit' | 'policyDate' | 'periodNumberingFrom' | 'cap' | 'proofThreshold' | 'noteThreshold' | 'reviewThreshold' | 'rounding' | 'graceDays' | 'entryDate' | 'pot' | 'alertThreshold' | 'escalateAfter' | 'parent' | `grace-${Grade}` | `proof-threshold-${Grade}` | PermAction>('clerkLimit');
  const [from, setFrom] = useState<Record<string, string>>({});
  const [error, setError] = useState('');
  const today = useSheetToday(sheetId);
  const dated = settings.data?.dated;
  const current = (key: string, starting = '') => (settings.data ? (valueOn(dated?.[key], today) ?? starting) : '');
  const currentTerms = current('terms', DEFAULT_TERMS);

  const [user] = useUser();
  const once = useSubmitOnce();
  const save = useMutation({
    mutationFn: (body: SettingsChange) =>
      putJson<SheetSettings>(`/api/sheets/${encodeURIComponent(sheetId)}/settings`, body, user),
    onSuccess: async () => {
      // The form is cleared before the re-reads, so nothing typed while they run is cleared after them.
      setLimit('');
      setFloor('');
      setTerms('');
      setGradeTerms({});
      setGradeGrace({});
      setGradeProof({});
      setLatePct('');
      setPolicyDate('');
      setNumberingFrom('');
      setCap('');
      setThreshold('');
      setNoteThreshold('');
      setReviewThreshold('');
      setRounding('');
      setGrace('');
      setEntryDate('');
      setPot('');
      setAlertThreshold('');
      setEscalateAfter('');
      setParent('');
      setFiscalStartMonth('');
      setPerms({});
      setFrom({});
      await queryClient.invalidateQueries({ queryKey: settingsKey(sheetId) });
      // A pool's cuts are figured from the pot.
      await queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) });
      // Where the sheet has no rate in force, its parent's is used.
      await queryClient.invalidateQueries({ queryKey: ratesKey(sheetId) });
    },
    onError: () => setError('The settings could not be saved.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const l = limit.trim();
    if (l !== '' && !AMOUNT.test(l)) {
      setError('The clerk limit must be an amount of 0.00 or more, with up to two decimals.');
      return;
    }
    const f = floor.trim();
    if (f !== '' && !AMOUNT.test(f)) {
      setError('The floor must be an amount of 0.00 or more, with up to two decimals.');
      return;
    }
    const t = terms.trim();
    if (t !== '' && !DAYS.test(t)) {
      setError('The terms must be a whole number of days, 0 or more.');
      return;
    }
    const g: Partial<Record<Grade, string>> = {};
    for (const grade of GRADES) {
      const days = (gradeTerms[grade] ?? '').trim();
      if (days === '') continue;
      if (!DAYS.test(days)) {
        setError(`The terms of grade ${grade} must be a whole number of days, 0 or more.`);
        return;
      }
      g[grade] = days;
    }
    const gg: Partial<Record<Grade, string>> = {};
    for (const grade of GRADES) {
      const days = (gradeGrace[grade] ?? '').trim();
      if (days !== '' && !DAYS.test(days)) {
        setError(`The grace days of grade ${grade} must be a whole number of days, 0 or more.`);
        return;
      }
      if (days !== '') gg[grade] = days;
    }
    const p = latePct.trim();
    if (p !== '' && !AMOUNT.test(p)) {
      setError('The late percent must be a percent of 0.00 or more, with up to two decimals.');
      return;
    }
    const d = policyDate.trim();
    if (d !== '' && !isDate(d)) {
      setError('The policy date must be a date (yyyy-mm-dd).');
      return;
    }
    const n = numberingFrom.trim();
    if (n !== '' && !isDate(n)) {
      setError('The period numbering from-date must be a date (yyyy-mm-dd).');
      return;
    }
    const c = cap.trim();
    if (c !== '' && !AMOUNT.test(c)) {
      setError('The cap must be an amount of 0.00 or more, with up to two decimals.');
      return;
    }
    const pt = threshold.trim();
    if (pt !== '' && !AMOUNT.test(pt)) {
      setError('The proof threshold must be an amount of 0.00 or more, with up to two decimals.');
      return;
    }
    const gp: Partial<Record<Grade, string>> = {};
    for (const grade of GRADES) {
      const amount = (gradeProof[grade] ?? '').trim();
      if (amount !== '' && !AMOUNT.test(amount)) {
        setError(`The proof threshold of grade ${grade} must be an amount of 0.00 or more, with up to two decimals.`);
        return;
      }
      if (amount !== '') gp[grade] = amount;
    }
    const nt = noteThreshold.trim();
    if (nt !== '' && !AMOUNT.test(nt)) {
      setError('The note threshold must be an amount of 0.00 or more, with up to two decimals.');
      return;
    }
    const rt = reviewThreshold.trim();
    if (rt !== '' && !AMOUNT.test(rt)) {
      setError('The review threshold must be an amount of 0.00 or more, with up to two decimals.');
      return;
    }
    const rm = rounding.trim();
    if (rm !== '' && !ROUNDING_MODES.includes(rm)) {
      setError('The rounding mode must be half-up or half-even.');
      return;
    }
    const gd = grace.trim();
    if (gd !== '' && !DAYS.test(gd)) {
      setError('The grace days must be a whole number of days, 0 or more.');
      return;
    }
    const ed = entryDate.trim();
    if (ed !== '' && !isDate(ed)) {
      setError('The entry date must be a date (yyyy-mm-dd).');
      return;
    }
    const pt2 = pot.trim();
    if (pt2 !== '' && !AMOUNT.test(pt2)) {
      setError('The pot must be an amount of 0.00 or more, with up to two decimals.');
      return;
    }
    const at = alertThreshold.trim();
    if (at !== '' && !AMOUNT.test(at)) {
      setError('The alert threshold must be an amount of 0.00 or more, with up to two decimals.');
      return;
    }
    const ea = escalateAfter.trim();
    if (ea !== '' && !DAYS.test(ea)) {
      setError('The escalation gap must be a whole number of days, 0 or more.');
      return;
    }
    const pa = parent.trim();
    if (pa === sheetId.trim()) {
      setError('A sheet cannot be its own parent.');
      return;
    }
    const fm = fiscalStartMonth.trim();
    if (fm !== '' && (!MONTH.test(fm) || Number(fm) < 1 || Number(fm) > 12)) {
      setError('The fiscal year start month must be a month, 1 to 12.');
      return;
    }
    const pm: Partial<Record<PermAction, string>> = {};
    for (const action of PERM_ACTIONS) {
      const roles = (perms[action] ?? '').trim();
      if (roles === '') continue;
      if (!PERM_ROLES.test(roles)) {
        setError(`The roles allowed to ${action} must be clerk or steward, separated by commas.`);
        return;
      }
      pm[action] = roles.split(',').map((r) => r.trim()).join(',');
    }
    const froms: Record<string, string> = {};
    for (const [key, date] of Object.entries(from)) {
      const d = date.trim();
      if (d === '') continue;
      if (!isDate(d)) {
        setError('A from-date must be a date (yyyy-mm-dd).');
        return;
      }
      froms[key] = d;
    }
    // A blank grace is none: saved when its from-date is given, or when it was the last field worked in.
    for (const grade of GRADES) {
      if (gg[grade] === undefined && (froms[`grace-${grade}`] ?? '') !== '') gg[grade] = '';
    }
    // A blank grade's proof threshold is none likewise.
    for (const grade of GRADES) {
      if (gp[grade] === undefined && (froms[`proof-threshold-${grade}`] ?? '') !== '') gp[grade] = '';
    }
    // A blank rounding mode is none likewise: saved when its from-date is given, or when it was the last field worked in.
    const roundingSaved = rm !== '' || (froms['rounding'] ?? '') !== '';
    // A blank review threshold is none likewise.
    const reviewSaved = rt !== '' || (froms['review-threshold'] ?? '') !== '';
    const body: SettingsChange = {};
    const none = !roundingSaved && !reviewSaved && l === '' && f === '' && t === '' && Object.keys(g).length === 0 && Object.keys(gg).length === 0 && Object.keys(gp).length === 0 && p === '' && d === '' && n === '' && c === '' && pt === '' && nt === '' && gd === '' && ed === '' && pt2 === '' && at === '' && ea === '' && pa === '' && fm === '' && Object.keys(pm).length === 0;
    if (l !== '' || (none && blank === 'clerkLimit')) body.clerkLimit = l;
    if (d !== '' || (none && blank === 'policyDate')) body.policyDate = d;
    if (n !== '' || (none && blank === 'periodNumberingFrom')) body.periodNumberingFrom = n;
    if (c !== '' || (none && blank === 'cap')) body.cap = c;
    if (pt !== '' || (none && blank === 'proofThreshold')) body.proofThreshold = pt;
    if (nt !== '' || (none && blank === 'noteThreshold')) body.noteThreshold = nt;
    if (roundingSaved || (none && blank === 'rounding')) body.rounding = rm;
    if (reviewSaved || (none && blank === 'reviewThreshold')) body.reviewThreshold = rt;
    if (gd !== '' || (none && blank === 'graceDays')) body.graceDays = gd;
    if (ed !== '' || (none && blank === 'entryDate')) body.entryDate = ed;
    if (pt2 !== '' || (none && blank === 'pot')) body.pot = pt2;
    if (at !== '' || (none && blank === 'alertThreshold')) body.alertThreshold = at;
    if (ea !== '' || (none && blank === 'escalateAfter')) body.escalateAfter = ea;
    if (pa !== '' || (none && blank === 'parent')) body.parent = pa;
    if (fm !== '') body.fiscalStartMonth = fm;
    for (const grade of GRADES) {
      if (none && blank === `grace-${grade}`) gg[grade] = '';
    }
    if (Object.keys(gg).length > 0) body.gradeGrace = gg;
    for (const grade of GRADES) {
      if (none && blank === `proof-threshold-${grade}`) gp[grade] = '';
    }
    if (Object.keys(gp).length > 0) body.gradeProof = gp;
    for (const action of PERM_ACTIONS) {
      if (pm[action] !== undefined || (none && blank === action)) body.perms = { ...body.perms, [action]: pm[action] ?? '' };
    }
    if (f !== '') body.floor = f;
    if (t !== '') body.terms = t;
    if (Object.keys(g).length > 0) body.gradeTerms = g;
    if (p !== '') body.latePct = p;
    if (Object.keys(froms).length > 0) body.from = froms;
    once(() => save.mutateAsync(body));
  };

  return (
    <section data-testid="settings">
      <h3>Settings</h3>
      <form data-testid="settings-form" onSubmit={submit}>
        <label>
          Clerk limit{' '}
          <input
            data-testid="setting-clerk-limit"
            placeholder="no limit"
            value={limit}
            onFocus={() => setBlank('clerkLimit')}
            onChange={(e) => {
              setLimit(e.target.value);
              setError('');
            }}
          />
        </label>{' '}
        <label>
          from{' '}
          <input
            data-testid={`setting-clerk-limit-from`}
            placeholder="sheet's today"
            value={from['clerk-limit'] ?? ''}
            onChange={(e) => {
              setFrom({ ...from, ['clerk-limit']: e.target.value });
              setError('');
            }}
          />
        </label>{' '}
        <label>
          Floor{' '}
          <input
            data-testid="setting-floor"
            placeholder="unchanged"
            value={floor}
            onChange={(e) => {
              setFloor(e.target.value);
              setError('');
            }}
          />
        </label>{' '}
        <label>
          from{' '}
          <input
            data-testid={`setting-floor-from`}
            placeholder="sheet's today"
            value={from['floor'] ?? ''}
            onChange={(e) => {
              setFrom({ ...from, ['floor']: e.target.value });
              setError('');
            }}
          />
        </label>{' '}
        <label>
          Terms (days){' '}
          <input
            data-testid="setting-terms"
            placeholder="unchanged"
            value={terms}
            onChange={(e) => {
              setTerms(e.target.value);
              setError('');
            }}
          />
        </label>{' '}
        <label>
          from{' '}
          <input
            data-testid={`setting-terms-from`}
            placeholder="sheet's today"
            value={from['terms'] ?? ''}
            onChange={(e) => {
              setFrom({ ...from, ['terms']: e.target.value });
              setError('');
            }}
          />
        </label>{' '}
        {GRADES.map((grade) => (
          <span key={grade}>
            <label>
              Terms {grade} (days){' '}
              <input
                data-testid={`setting-terms-${grade}`}
                placeholder="unchanged"
                value={gradeTerms[grade] ?? ''}
                onChange={(e) => {
                  setGradeTerms({ ...gradeTerms, [grade]: e.target.value });
                  setError('');
                }}
              />
            </label>{' '}
            <label>
              from{' '}
              <input
                data-testid={`setting-terms-${grade}-from`}
                placeholder="sheet's today"
                value={from[`terms-${grade}`] ?? ''}
                onChange={(e) => {
                  setFrom({ ...from, [`terms-${grade}`]: e.target.value });
                  setError('');
                }}
              />
            </label>{' '}
          </span>
        ))}
        <label>
          Late percent{' '}
          <input
            data-testid="setting-late-pct"
            placeholder="unchanged"
            value={latePct}
            onChange={(e) => {
              setLatePct(e.target.value);
              setError('');
            }}
          />
        </label>{' '}
        <label>
          from{' '}
          <input
            data-testid={`setting-late-pct-from`}
            placeholder="sheet's today"
            value={from['late-pct'] ?? ''}
            onChange={(e) => {
              setFrom({ ...from, ['late-pct']: e.target.value });
              setError('');
            }}
          />
        </label>{' '}
        <label>
          Policy date{' '}
          <input
            data-testid="setting-policy-date"
            placeholder="sheet's today"
            value={policyDate}
            onFocus={() => setBlank('policyDate')}
            onChange={(e) => {
              setPolicyDate(e.target.value);
              setError('');
            }}
          />
        </label>{' '}
        <label>
          Period numbering from{' '}
          <input
            data-testid="setting-period-numbering-from"
            placeholder="off"
            value={numberingFrom}
            onFocus={() => setBlank('periodNumberingFrom')}
            onChange={(e) => {
              setNumberingFrom(e.target.value);
              setError('');
            }}
          />
        </label>{' '}
        <label>
          Cap{' '}
          <input
            data-testid="setting-cap"
            placeholder="none"
            value={cap}
            onFocus={() => setBlank('cap')}
            onChange={(e) => {
              setCap(e.target.value);
              setError('');
            }}
          />
        </label>{' '}
        <label>
          from{' '}
          <input
            data-testid={`setting-cap-from`}
            placeholder="sheet's today"
            value={from['cap'] ?? ''}
            onChange={(e) => {
              setFrom({ ...from, ['cap']: e.target.value });
              setError('');
            }}
          />
        </label>{' '}
        <label>
          Proof threshold{' '}
          <input
            data-testid="setting-proof-threshold"
            placeholder="none"
            value={threshold}
            onFocus={() => setBlank('proofThreshold')}
            onChange={(e) => {
              setThreshold(e.target.value);
              setError('');
            }}
          />
        </label>{' '}
        <label>
          from{' '}
          <input
            data-testid={`setting-proof-threshold-from`}
            placeholder="sheet's today"
            value={from['proof-threshold'] ?? ''}
            onChange={(e) => {
              setFrom({ ...from, ['proof-threshold']: e.target.value });
              setError('');
            }}
          />
        </label>{' '}
        {GRADES.map((grade) => (
          <span key={grade}>
            <label>
              Proof threshold {grade}{' '}
              <input
                data-testid={`setting-proof-threshold-${grade}`}
                placeholder="sheet's"
                value={gradeProof[grade] ?? ''}
                onFocus={() => setBlank(`proof-threshold-${grade}`)}
                onChange={(e) => {
                  setGradeProof({ ...gradeProof, [grade]: e.target.value });
                  setError('');
                }}
              />
            </label>{' '}
            <label>
              from{' '}
              <input
                data-testid={`setting-proof-threshold-${grade}-from`}
                placeholder="sheet's today"
                value={from[`proof-threshold-${grade}`] ?? ''}
                onChange={(e) => {
                  setFrom({ ...from, [`proof-threshold-${grade}`]: e.target.value });
                  setError('');
                }}
              />
            </label>{' '}
          </span>
        ))}
        <label>
          Note threshold{' '}
          <input
            data-testid="setting-note-threshold"
            placeholder="none"
            value={noteThreshold}
            onFocus={() => setBlank('noteThreshold')}
            onChange={(e) => {
              setNoteThreshold(e.target.value);
              setError('');
            }}
          />
        </label>{' '}
        <label>
          from{' '}
          <input
            data-testid={`setting-note-threshold-from`}
            placeholder="sheet's today"
            value={from['note-threshold'] ?? ''}
            onChange={(e) => {
              setFrom({ ...from, ['note-threshold']: e.target.value });
              setError('');
            }}
          />
        </label>{' '}
        <label>
          Review threshold{' '}
          <input
            data-testid="setting-review-threshold"
            placeholder="none"
            value={reviewThreshold}
            onFocus={() => setBlank('reviewThreshold')}
            onChange={(e) => {
              setReviewThreshold(e.target.value);
              setError('');
            }}
          />
        </label>{' '}
        <label>
          from{' '}
          <input
            data-testid={`setting-review-threshold-from`}
            placeholder="sheet's today"
            value={from['review-threshold'] ?? ''}
            onChange={(e) => {
              setFrom({ ...from, ['review-threshold']: e.target.value });
              setError('');
            }}
          />
        </label>{' '}
        <label>
          Rounding{' '}
          <input
            data-testid="setting-rounding"
            placeholder="none (half-up or half-even)"
            value={rounding}
            onFocus={() => setBlank('rounding')}
            onChange={(e) => {
              setRounding(e.target.value);
              setError('');
            }}
          />
        </label>{' '}
        <label>
          from{' '}
          <input
            data-testid="setting-rounding-from"
            placeholder="sheet's today"
            value={from['rounding'] ?? ''}
            onChange={(e) => {
              setFrom({ ...from, ['rounding']: e.target.value });
              setError('');
            }}
          />
        </label>{' '}
        <label>
          Grace days{' '}
          <input
            data-testid="setting-grace-days"
            placeholder="off"
            value={grace}
            onFocus={() => setBlank('graceDays')}
            onChange={(e) => {
              setGrace(e.target.value);
              setError('');
            }}
          />
        </label>{' '}
        {GRADES.map((grade) => (
          <span key={grade}>
            <label>
              Grace {grade} (days){' '}
              <input
                data-testid={`setting-grace-${grade}`}
                placeholder="none"
                value={gradeGrace[grade] ?? ''}
                onFocus={() => setBlank(`grace-${grade}`)}
                onChange={(e) => {
                  setGradeGrace({ ...gradeGrace, [grade]: e.target.value });
                  setError('');
                }}
              />
            </label>{' '}
            <label>
              from{' '}
              <input
                data-testid={`setting-grace-${grade}-from`}
                placeholder="sheet's today"
                value={from[`grace-${grade}`] ?? ''}
                onChange={(e) => {
                  setFrom({ ...from, [`grace-${grade}`]: e.target.value });
                  setError('');
                }}
              />
            </label>{' '}
          </span>
        ))}
        <label>
          Entry date{' '}
          <input
            data-testid="setting-entry-date"
            placeholder="off"
            value={entryDate}
            onFocus={() => setBlank('entryDate')}
            onChange={(e) => {
              setEntryDate(e.target.value);
              setError('');
            }}
          />
        </label>{' '}
        <label>
          Pot{' '}
          <input
            data-testid="setting-pot"
            placeholder="none"
            value={pot}
            onFocus={() => setBlank('pot')}
            onChange={(e) => {
              setPot(e.target.value);
              setError('');
            }}
          />
        </label>{' '}
        <label>
          Alert threshold{' '}
          <input
            data-testid="setting-alert-threshold"
            placeholder="off"
            value={alertThreshold}
            onFocus={() => setBlank('alertThreshold')}
            onChange={(e) => {
              setAlertThreshold(e.target.value);
              setError('');
            }}
          />
        </label>{' '}
        <label>
          Escalate after (days){' '}
          <input
            data-testid="setting-escalate-after"
            placeholder="off"
            value={escalateAfter}
            onFocus={() => setBlank('escalateAfter')}
            onChange={(e) => {
              setEscalateAfter(e.target.value);
              setError('');
            }}
          />
        </label>{' '}
        <label>
          Parent sheet{' '}
          <input
            data-testid="setting-parent"
            placeholder="none"
            value={parent}
            onFocus={() => setBlank('parent')}
            onChange={(e) => {
              setParent(e.target.value);
              setError('');
            }}
          />
        </label>{' '}
        <label>
          Fiscal year start month{' '}
          <input
            data-testid="setting-fiscal-start-month"
            placeholder="1 to 12"
            value={fiscalStartMonth}
            onChange={(e) => {
              setFiscalStartMonth(e.target.value);
              setError('');
            }}
          />
        </label>{' '}
        {PERM_ACTIONS.map((action) => (
          <span key={action}>
            <label>
              Roles allowed to {action}{' '}
              <input
                data-testid={`setting-perm-${action}`}
                placeholder="standing rule"
                value={perms[action] ?? ''}
                onFocus={() => setBlank(action)}
                onChange={(e) => {
                  setPerms({ ...perms, [action]: e.target.value });
                  setError('');
                }}
              />
            </label>{' '}
          </span>
        ))}
        <button type="submit" data-testid="settings-save" disabled={save.isPending}>
          Save settings
        </button>
        {error && <p data-testid="settings-error">{error}</p>}
      </form>
      {settings.isError && <p data-testid="settings-load-error">Could not load the settings.</p>}
      <p>
        Clerk limit in force: <span data-testid="setting-clerk-limit-current">{current('clerk-limit')}</span>
      </p>
      <p>
        Floor in force: <span data-testid="setting-floor-current">{current('floor', DEFAULT_FLOOR)}</span>
      </p>
      <p>
        Terms in force: <span data-testid="setting-terms-current">{currentTerms}</span>
      </p>
      {GRADES.map((grade) => (
        <p key={grade}>
          Terms of grade {grade} in force:{' '}
          <span data-testid={`setting-terms-${grade}-current`}>{current(`terms-${grade}`, currentTerms)}</span>
        </p>
      ))}
      <p>
        Late percent in force: <span data-testid="setting-late-pct-current">{current('late-pct', DEFAULT_LATE_PCT)}</span>
      </p>
      <p>
        Policy date in force: <span data-testid="setting-policy-date-current">{settings.data?.policyDate ?? ''}</span>
      </p>
      <p>
        Period numbering from:{' '}
        <span data-testid="setting-period-numbering-from-current">{settings.data?.periodNumberingFrom ?? ''}</span>
      </p>
      <p>
        Cap in force: <span data-testid="setting-cap-current">{current('cap')}</span>
      </p>
      <p>
        Proof threshold in force: <span data-testid="setting-proof-threshold-current">{current('proof-threshold')}</span>
      </p>
      {GRADES.map((grade) => (
        <p key={grade}>
          Proof threshold of grade {grade} in force:{' '}
          <span data-testid={`setting-proof-threshold-${grade}-current`}>{current(`proof-threshold-${grade}`)}</span>
        </p>
      ))}
      <p>
        Note threshold in force: <span data-testid="setting-note-threshold-current">{current('note-threshold')}</span>
      </p>
      <p>
        Review threshold in force: <span data-testid="setting-review-threshold-current">{current('review-threshold')}</span>
      </p>
      <p>
        Rounding in force: <span data-testid="setting-rounding-current">{current('rounding')}</span>
      </p>
      <p>
        Grace days: <span data-testid="setting-grace-days-current">{settings.data?.graceDays ?? ''}</span>
      </p>
      {GRADES.map((grade) => (
        <p key={grade}>
          Grace of grade {grade} in force: <span data-testid={`setting-grace-${grade}-current`}>{current(`grace-${grade}`)}</span>
        </p>
      ))}
      <p>
        Entry date: <span data-testid="setting-entry-date-current">{settings.data?.entryDate ?? ''}</span>
      </p>
      <p>
        Pot: <span data-testid="setting-pot-current">{settings.data?.pot ?? ''}</span>
      </p>
      <p>
        Alert threshold: <span data-testid="setting-alert-threshold-current">{settings.data?.alertThreshold ?? ''}</span>
      </p>
      <p>
        Escalate after (days): <span data-testid="setting-escalate-after-current">{settings.data?.escalateAfter ?? ''}</span>
      </p>
      <p>
        Parent sheet: <span data-testid="setting-parent-current">{settings.data?.parent ?? ''}</span>
      </p>
      <p>
        Fiscal year start month:{' '}
        <span data-testid="setting-fiscal-start-month-current">{settings.data?.fiscalStartMonth ?? ''}</span>
      </p>
      {PERM_ACTIONS.map((action) => (
        <p key={action}>
          Roles allowed to {action}: <span data-testid={`setting-perm-${action}-current`}>{settings.data?.perms?.[action] ?? ''}</span>
        </p>
      ))}
    </section>
  );
}

/** The roles the sheet gives user names (any name not listed is a clerk), and the form a role is
 *  set through. */
function Roles({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const roles = useQuery(rolesQuery(sheetId));
  const [name, setName] = useState('');
  const [role, setRole] = useState('');
  const [error, setError] = useState('');

  const [user] = useUser();
  const once = useSubmitOnce();
  const save = useMutation({
    mutationFn: (body: Role) =>
      putJson<Role>(
        `/api/sheets/${encodeURIComponent(sheetId)}/roles/${encodeURIComponent(body.user)}`,
        { role: body.role },
        user,
      ),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: rolesKey(sheetId) });
      setName('');
      setRole('');
    },
    onError: () => setError('The role could not be set.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const n = name.trim();
    const r = role.trim();
    if (n === '') {
      setError('A user name is needed.');
      return;
    }
    if (!ROLES.includes(r as RoleName)) {
      setError('The role must be clerk, steward or viewer.');
      return;
    }
    once(() => save.mutateAsync({ user: n, role: r as RoleName }));
  };

  return (
    <section data-testid="roles">
      <h3>Roles</h3>
      <form data-testid="role-form" onSubmit={submit}>
        <label>
          User{' '}
          <input
            data-testid="role-form-user"
            value={name}
            onChange={(e) => {
              setName(e.target.value);
              setError('');
            }}
          />
        </label>{' '}
        <label>
          Role{' '}
          <input
            data-testid="role-form-role"
            placeholder="clerk, steward or viewer"
            list="role-names"
            value={role}
            onChange={(e) => {
              setRole(e.target.value);
              setError('');
            }}
          />
          <datalist id="role-names">
            {ROLES.map((r) => (
              <option key={r} value={r} />
            ))}
          </datalist>
        </label>{' '}
        <button type="submit" data-testid="role-form-submit" disabled={save.isPending}>
          Set role
        </button>
        {error && <p data-testid="role-form-error">{error}</p>}
      </form>
      {roles.isError && <p data-testid="roles-load-error">Could not load the roles.</p>}
      <table data-testid="roles-list">
        <tbody>
          {(roles.data ?? []).map((r) => (
            <tr key={r.user} data-testid={`role-row-${r.user}`}>
              <td data-testid="role-user">{r.user}</td>
              <td data-testid="role-value">{r.role}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

// Keyed by sheet, so a typed value or an error never carries over to another sheet.
export const contribution = SheetPanel.fill({
  order: 6,
  Component: ({ sheetId }) => (
    <>
      <Settings key={`settings-${sheetId}`} sheetId={sheetId} />
      <Roles key={`roles-${sheetId}`} sheetId={sheetId} />
    </>
  ),
});
