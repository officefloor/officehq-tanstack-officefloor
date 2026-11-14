import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { sheetInfoKey, useCurrentSheet, useCurrentUser } from '../session/session';
import { poolsKey } from '../pools/pools';
import { ratesKey } from '../rates/rates';
import {
  AMOUNT,
  DATE,
  DAYS,
  DEFAULT_FLOOR,
  DEFAULT_LATE_PCT,
  DEFAULT_TERMS,
  PERCENT,
  PERMISSION_ACTIONS,
  ROLE_LIST,
  STANDING_PERMISSIONS,
  settingsKey,
  useSettings,
  valueOn,
} from './settings';
import { useSheetToday } from '../session/sheetToday';
import type { Settings } from './settings';
import { useOneAtATime } from '../../query/useOneAtATime';
import { GRADES } from '../people/people';

/** The settings the form sends: only those whose field was touched since the last save. */
type SettingsInput = {
  clerkLimit?: string;
  floor?: string;
  terms?: string;
  gradeTerms?: Record<string, string>;
  latePct?: string;
  policyDate?: string;
  periodNumberingFrom?: string;
  entryDate?: string;
  cap?: string;
  proofThreshold?: string;
  noteThreshold?: string;
  clerkLimitFrom?: string;
  floorFrom?: string;
  termsFrom?: string;
  latePctFrom?: string;
  capFrom?: string;
  proofThresholdFrom?: string;
  noteThresholdFrom?: string;
  gradeTermsFrom?: Record<string, string>;
  gradeGrace?: Record<string, string>;
  gradeGraceFrom?: Record<string, string>;
  gradeProofThreshold?: Record<string, string>;
  gradeProofThresholdFrom?: Record<string, string>;
  permissions?: Record<string, string>;
  graceDays?: string;
  pot?: string;
  parent?: string;
  fiscalStartMonth?: string;
  alertThreshold?: string;
};

/** The from-date field of each dated setting, by its key as the screen names it (terms-A for a grade's terms). */
type Froms = Record<string, string>;

type Touched = {
  clerkLimit: boolean;
  floor: boolean;
  terms: boolean;
  latePct: boolean;
  policyDate: boolean;
  periodNumberingFrom: boolean;
  entryDate: boolean;
  cap: boolean;
  proofThreshold: boolean;
  noteThreshold: boolean;
  graceDays: boolean;
  pot: boolean;
  parent: boolean;
  fiscalStartMonth: boolean;
  alertThreshold: boolean;
};

const UNTOUCHED: Touched = {
  clerkLimit: false,
  floor: false,
  terms: false,
  latePct: false,
  policyDate: false,
  periodNumberingFrom: false,
  entryDate: false,
  cap: false,
  proofThreshold: false,
  noteThreshold: false,
  graceDays: false,
  pot: false,
  parent: false,
  fiscalStartMonth: false,
  alertThreshold: false,
};

// The sheet's clerk limit (blank: no limit), floor, terms, each grade's terms (blank: the sheet's), late percent and policy date (blank: today) and period numbering from date (blank: off) and entry date (blank: off) and cap (blank: none) and proof threshold (blank: none) and each grade's proof threshold (blank: the sheet's) and note threshold (blank: none) in force, and the
// permission matrix (by action, the roles allowed it; blank: the standing rule; not dated), and the grace days (blank: off; not dated), and the pot (blank: none; not dated), and the parent sheet (blank: none; not dated), and the fiscal year start month (1 to 12; blank: 1; not dated), and the alert threshold (blank: off; not dated), and the form that sets them. Saving sends
// only the fields touched since the last save, so a blank field left alone changes nothing. A dated setting (the clerk
// limit, floor, terms, each grade's terms, late percent, cap, proof threshold, each grade's proof threshold and note threshold) is recorded from the date typed beside it (blank: the
// sheet's today); the value shown is the one in force on the sheet's today.
export function SettingsForm() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const settings = useSettings().data;
  const today = useSheetToday();
  const [gradeGrace, setGradeGrace] = useState<Record<string, string>>({});
  const [touchedGrace, setTouchedGrace] = useState<Record<string, boolean>>({});
  const [gradeProof, setGradeProof] = useState<Record<string, string>>({});
  const [touchedProof, setTouchedProof] = useState<Record<string, boolean>>({});
  const [clerkLimit, setClerkLimit] = useState('');
  const [floor, setFloor] = useState('');
  const [terms, setTerms] = useState('');
  const [gradeTerms, setGradeTerms] = useState<Record<string, string>>({});
  const [touchedGrades, setTouchedGrades] = useState<Record<string, boolean>>({});
  const [latePct, setLatePct] = useState('');
  const [policyDate, setPolicyDate] = useState('');
  const [periodNumberingFrom, setPeriodNumberingFrom] = useState('');
  const [entryDate, setEntryDate] = useState('');
  const [cap, setCap] = useState('');
  const [proofThreshold, setProofThreshold] = useState('');
  const [noteThreshold, setNoteThreshold] = useState('');
  const [graceDays, setGraceDays] = useState('');
  const [pot, setPot] = useState('');
  const [parent, setParent] = useState('');
  const [fiscalStartMonth, setFiscalStartMonth] = useState('');
  const [alertThreshold, setAlertThreshold] = useState('');
  const [perms, setPerms] = useState<Record<string, string>>({});
  const [touchedPerms, setTouchedPerms] = useState<Record<string, boolean>>({});
  const [froms, setFroms] = useState<Froms>({});
  const [touched, setTouched] = useState<Touched>(UNTOUCHED);
  const [error, setError] = useState('');
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const save = useMutation({
    mutationFn: (input: SettingsInput) =>
      putJson<Settings>(`/api/sheets/${encodeURIComponent(sheet)}/settings?user=${encodeURIComponent(user)}`, input),
    onSuccess: async (saved) => {
      setClerkLimit('');
      setFloor('');
      setTerms('');
      setGradeTerms({});
      setTouchedGrades({});
      setGradeGrace({});
      setTouchedGrace({});
      setGradeProof({});
      setTouchedProof({});
      setLatePct('');
      setPolicyDate('');
      setPeriodNumberingFrom('');
      setEntryDate('');
      setCap('');
      setProofThreshold('');
      setNoteThreshold('');
      setGraceDays('');
      setPot('');
      setParent('');
      setFiscalStartMonth('');
      setAlertThreshold('');
      setPerms({});
      setTouchedPerms({});
      setFroms({});
      setTouched(UNTOUCHED);
      setError('');
      queryClient.setQueryData(settingsKey(sheet), saved);
      await queryClient.invalidateQueries({ queryKey: settingsKey(sheet) });
      await queryClient.invalidateQueries({ queryKey: poolsKey(sheet) });
      await queryClient.invalidateQueries({ queryKey: ratesKey(sheet) });
      await queryClient.invalidateQueries({ queryKey: sheetInfoKey(sheet) });
    },
    onError: () => setError('That setting could not be saved.'),
  });

  if (!user || !sheet) return null;

  // The from-date typed for a dated setting: '' (the sheet's today), a date, or null when it is not a date.
  const fromOf = (key: string): string | null => {
    const value = (froms[key] ?? '').trim();
    return value === '' || DATE.test(value) ? value : null;
  };

  // The from-date field beside a dated setting's field.
  const fromField = (key: string, label: string) => (
    <input
      data-testid={`setting-${key}-from`}
      placeholder={`${label} from, yyyy-MM-dd (blank: the sheet's today)`}
      value={froms[key] ?? ''}
      onChange={(e) => {
        const value = e.target.value;
        setFroms((f) => ({ ...f, [key]: value }));
        setError('');
      }}
    />
  );

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const input: SettingsInput = {};
    for (const [key, label] of [
      ['clerk-limit', 'clerk limit'],
      ['floor', 'floor'],
      ['terms', 'terms'],
      ...GRADES.map((g) => [`terms-${g}`, `terms for grade ${g}`]),
      ...GRADES.map((g) => [`grace-${g}`, `grace days for grade ${g}`]),
      ['late-pct', 'late percent'],
      ['cap', 'cap'],
      ['proof-threshold', 'proof threshold'],
      ...GRADES.map((g) => [`proof-threshold-${g}`, `proof threshold for grade ${g}`]),
      ['note-threshold', 'note threshold'],
    ]) {
      if (fromOf(key) === null) {
        setError(`Type the ${label} from-date as yyyy-MM-dd, or leave it blank for the sheet's today.`);
        return;
      }
    }
    const datedFrom = (key: string) => fromOf(key) || undefined;
    if (touched.clerkLimit) {
      const value = clerkLimit.trim();
      if (value !== '' && !AMOUNT.test(value)) {
        setError('Type a clerk limit of 0.00 or more, with at most two decimals, or leave it blank for no limit.');
        return;
      }
      input.clerkLimit = value;
      input.clerkLimitFrom = datedFrom('clerk-limit');
    }
    if (touched.floor) {
      const value = floor.trim();
      if (value !== '' && !AMOUNT.test(value)) {
        setError(`Type a floor of 0.00 or more, with at most two decimals, or leave it blank for ${DEFAULT_FLOOR}.`);
        return;
      }
      input.floor = value;
      input.floorFrom = datedFrom('floor');
    }
    if (touched.terms) {
      const value = terms.trim();
      if (value !== '' && !DAYS.test(value)) {
        setError(`Type terms of 0 or more whole days, or leave them blank for ${DEFAULT_TERMS}.`);
        return;
      }
      input.terms = value;
      input.termsFrom = datedFrom('terms');
    }
    for (const g of GRADES) {
      if (!touchedGrades[g]) continue;
      const value = (gradeTerms[g] ?? '').trim();
      if (value !== '' && !DAYS.test(value)) {
        setError(`Type terms for grade ${g} of 0 or more whole days, or leave them blank for the sheet's terms.`);
        return;
      }
      input.gradeTerms = { ...input.gradeTerms, [g]: value };
      const from = datedFrom(`terms-${g}`);
      if (from) input.gradeTermsFrom = { ...input.gradeTermsFrom, [g]: from };
    }
    for (const g of GRADES) {
      if (!touchedGrace[g]) continue;
      const value = (gradeGrace[g] ?? '').trim();
      if (value !== '' && !DAYS.test(value)) {
        setError(`Type grace days for grade ${g} of 0 or more whole days, or leave them blank for none.`);
        return;
      }
      input.gradeGrace = { ...input.gradeGrace, [g]: value };
      const from = datedFrom(`grace-${g}`);
      if (from) input.gradeGraceFrom = { ...input.gradeGraceFrom, [g]: from };
    }
    if (touched.latePct) {
      const value = latePct.trim();
      if (value !== '' && !PERCENT.test(value)) {
        setError(
          `Type a late percent of 0.00 or more, with at most two decimals, or leave it blank for ${DEFAULT_LATE_PCT}.`,
        );
        return;
      }
      input.latePct = value;
      input.latePctFrom = datedFrom('late-pct');
    }
    if (touched.policyDate) {
      const value = policyDate.trim();
      if (value !== '' && !DATE.test(value)) {
        setError("Type a policy date as yyyy-MM-dd, or leave it blank for the sheet's today.");
        return;
      }
      input.policyDate = value;
    }
    if (touched.periodNumberingFrom) {
      const value = periodNumberingFrom.trim();
      if (value !== '' && !DATE.test(value)) {
        setError('Type a period numbering from date as yyyy-MM-dd, or leave it blank for off.');
        return;
      }
      input.periodNumberingFrom = value;
    }
    if (touched.entryDate) {
      const value = entryDate.trim();
      if (value !== '' && !DATE.test(value)) {
        setError('Type an entry date as yyyy-MM-dd, or leave it blank for off.');
        return;
      }
      input.entryDate = value;
    }
    if (touched.cap) {
      const value = cap.trim();
      if (value !== '' && !AMOUNT.test(value)) {
        setError('Type a cap of 0.00 or more, with at most two decimals, or leave it blank for none.');
        return;
      }
      input.cap = value;
      input.capFrom = datedFrom('cap');
    }
    if (touched.proofThreshold) {
      const value = proofThreshold.trim();
      if (value !== '' && !AMOUNT.test(value)) {
        setError('Type a proof threshold of 0.00 or more, with at most two decimals, or leave it blank for none.');
        return;
      }
      input.proofThreshold = value;
      input.proofThresholdFrom = datedFrom('proof-threshold');
    }
    for (const g of GRADES) {
      if (!touchedProof[g]) continue;
      const value = (gradeProof[g] ?? '').trim();
      if (value !== '' && !AMOUNT.test(value)) {
        setError(
          `Type a proof threshold for grade ${g} of 0.00 or more, with at most two decimals, or leave it blank for the sheet's.`,
        );
        return;
      }
      input.gradeProofThreshold = { ...input.gradeProofThreshold, [g]: value };
      const from = datedFrom(`proof-threshold-${g}`);
      if (from) input.gradeProofThresholdFrom = { ...input.gradeProofThresholdFrom, [g]: from };
    }
    if (touched.noteThreshold) {
      const value = noteThreshold.trim();
      if (value !== '' && !AMOUNT.test(value)) {
        setError('Type a note threshold of 0.00 or more, with at most two decimals, or leave it blank for none.');
        return;
      }
      input.noteThreshold = value;
      input.noteThresholdFrom = datedFrom('note-threshold');
    }
    if (touched.graceDays) {
      const value = graceDays.trim();
      if (value !== '' && !DAYS.test(value)) {
        setError('Type grace days of 0 or more whole days, or leave them blank for off.');
        return;
      }
      input.graceDays = value;
    }
    if (touched.pot) {
      const value = pot.trim();
      if (value !== '' && !AMOUNT.test(value)) {
        setError('Type a pot of 0.00 or more, with at most two decimals, or leave it blank for none.');
        return;
      }
      input.pot = value;
    }
    if (touched.parent) {
      input.parent = parent.trim();
    }
    if (touched.fiscalStartMonth) {
      const value = fiscalStartMonth.trim();
      if (value !== '' && !(/^\d{1,2}$/.test(value) && Number(value) >= 1 && Number(value) <= 12)) {
        setError('Type a fiscal year start month from 1 to 12, or leave it blank for 1.');
        return;
      }
      input.fiscalStartMonth = value;
    }
    if (touched.alertThreshold) {
      const value = alertThreshold.trim();
      if (value !== '' && !AMOUNT.test(value)) {
        setError('Type an alert threshold of 0.00 or more, with at most two decimals, or leave it blank for off.');
        return;
      }
      input.alertThreshold = value;
    }
    for (const a of PERMISSION_ACTIONS) {
      if (!touchedPerms[a]) continue;
      const value = (perms[a] ?? '').trim();
      if (value !== '' && !ROLE_LIST.test(value)) {
        setError(`Type the roles allowed to ${a} as clerk or steward, comma separated, or leave it blank for the standing rule.`);
        return;
      }
      input.permissions = { ...input.permissions, [a]: value };
    }
    if (
      input.clerkLimit === undefined &&
      input.floor === undefined &&
      input.terms === undefined &&
      input.gradeTerms === undefined &&
      input.gradeGrace === undefined &&
      input.latePct === undefined &&
      input.policyDate === undefined &&
      input.periodNumberingFrom === undefined &&
      input.entryDate === undefined &&
      input.cap === undefined &&
      input.proofThreshold === undefined &&
      input.gradeProofThreshold === undefined &&
      input.noteThreshold === undefined &&
      input.permissions === undefined &&
      input.graceDays === undefined &&
      input.pot === undefined &&
      input.parent === undefined &&
      input.fiscalStartMonth === undefined &&
      input.alertThreshold === undefined
    )
      return;
    once(() => save.mutateAsync(input));
  };

  return (
    <section data-testid="settings">
      <p>
        Clerk limit: <span data-testid="setting-clerk-limit-current">{settings?.clerkLimit ?? ''}</span>
      </p>
      <p>
        Floor: <span data-testid="setting-floor-current">{settings?.floor ?? ''}</span>
      </p>
      <p>
        Terms (days): <span data-testid="setting-terms-current">{settings?.terms ?? ''}</span>
      </p>
      {GRADES.map((g) => (
        <p key={g}>
          Terms for grade {g} (days):{' '}
          <span data-testid={`setting-terms-${g}-current`}>
            {settings ? (settings.gradeTerms[g] ?? settings.terms) : ''}
          </span>
        </p>
      ))}
      {GRADES.map((g) => (
        <p key={g}>
          Grace days for grade {g}:{' '}
          <span data-testid={`setting-grace-${g}-current`}>
            {settings ? valueOn(settings.history[`grace-${g}`], today, '') : ''}
          </span>
        </p>
      ))}
      <p>
        Late percent: <span data-testid="setting-late-pct-current">{settings?.latePct ?? ''}</span>
      </p>
      <p>
        Policy date: <span data-testid="setting-policy-date-current">{settings?.policyDate ?? ''}</span>
      </p>
      <p>
        Period numbering from:{' '}
        <span data-testid="setting-period-numbering-from-current">{settings?.periodNumberingFrom ?? ''}</span>
      </p>
      <p>
        Entry date: <span data-testid="setting-entry-date-current">{settings?.entryDate ?? ''}</span>
      </p>
      <p>
        Cap: <span data-testid="setting-cap-current">{settings?.cap ?? ''}</span>
      </p>
      <p>
        Proof threshold: <span data-testid="setting-proof-threshold-current">{settings?.proofThreshold ?? ''}</span>
      </p>
      {GRADES.map((g) => (
        <p key={g}>
          Proof threshold for grade {g}:{' '}
          <span data-testid={`setting-proof-threshold-${g}-current`}>
            {settings ? valueOn(settings.history[`proof-threshold-${g}`], today, '') : ''}
          </span>
        </p>
      ))}
      <p>
        Note threshold: <span data-testid="setting-note-threshold-current">{settings?.noteThreshold ?? ''}</span>
      </p>
      <p>
        Grace days: <span data-testid="setting-grace-days-current">{settings?.graceDays ?? ''}</span>
      </p>
      <p>
        Pot: <span data-testid="setting-pot-current">{settings?.pot ?? ''}</span>
      </p>
      <p>
        Parent sheet: <span data-testid="setting-parent-current">{settings?.parent ?? ''}</span>
      </p>
      <p>
        Fiscal year start month:{' '}
        <span data-testid="setting-fiscal-start-month-current">{settings?.fiscalStartMonth ?? ''}</span>
      </p>
      <p>
        Alert threshold: <span data-testid="setting-alert-threshold-current">{settings?.alertThreshold ?? ''}</span>
      </p>
      {PERMISSION_ACTIONS.map((a) => (
        <p key={a}>
          Roles allowed to {a}: <span data-testid={`setting-perm-${a}-current`}>{settings?.permissions?.[a] ?? ''}</span>
        </p>
      ))}
      <form onSubmit={submit} data-testid="settings-form">
        <input
          data-testid="setting-clerk-limit"
          placeholder="Clerk limit (blank: no limit)"
          value={clerkLimit}
          onFocus={() => setTouched((t) => ({ ...t, clerkLimit: true }))}
          onChange={(e) => {
            setClerkLimit(e.target.value);
            setTouched((t) => ({ ...t, clerkLimit: true }));
            setError('');
          }}
        />
        {fromField('clerk-limit', 'Clerk limit')}
        <input
          data-testid="setting-floor"
          placeholder={`Floor (blank: ${DEFAULT_FLOOR})`}
          value={floor}
          onFocus={() => setTouched((t) => ({ ...t, floor: true }))}
          onChange={(e) => {
            setFloor(e.target.value);
            setTouched((t) => ({ ...t, floor: true }));
            setError('');
          }}
        />
        {fromField('floor', 'Floor')}
        <input
          data-testid="setting-terms"
          placeholder={`Terms in days (blank: ${DEFAULT_TERMS})`}
          value={terms}
          onFocus={() => setTouched((t) => ({ ...t, terms: true }))}
          onChange={(e) => {
            setTerms(e.target.value);
            setTouched((t) => ({ ...t, terms: true }));
            setError('');
          }}
        />
        {fromField('terms', 'Terms')}
        {GRADES.map((g) => (
          <span key={g}>
            <input
              data-testid={`setting-terms-${g}`}
              placeholder={`Terms for grade ${g} in days (blank: the sheet's)`}
              value={gradeTerms[g] ?? ''}
              onFocus={() => setTouchedGrades((t) => ({ ...t, [g]: true }))}
              onChange={(e) => {
                setGradeTerms((v) => ({ ...v, [g]: e.target.value }));
                setTouchedGrades((t) => ({ ...t, [g]: true }));
                setError('');
              }}
            />
            {fromField(`terms-${g}`, `Terms for grade ${g}`)}
          </span>
        ))}
        {GRADES.map((g) => (
          <span key={g}>
            <input
              data-testid={`setting-grace-${g}`}
              placeholder={`Grace days for grade ${g} after a claim's due-by (blank: none)`}
              value={gradeGrace[g] ?? ''}
              onFocus={() => setTouchedGrace((t) => ({ ...t, [g]: true }))}
              onChange={(e) => {
                setGradeGrace((v) => ({ ...v, [g]: e.target.value }));
                setTouchedGrace((t) => ({ ...t, [g]: true }));
                setError('');
              }}
            />
            {fromField(`grace-${g}`, `Grace days for grade ${g}`)}
          </span>
        ))}
        <input
          data-testid="setting-late-pct"
          placeholder={`Late percent every 30 days overdue (blank: ${DEFAULT_LATE_PCT})`}
          value={latePct}
          onFocus={() => setTouched((t) => ({ ...t, latePct: true }))}
          onChange={(e) => {
            setLatePct(e.target.value);
            setTouched((t) => ({ ...t, latePct: true }));
            setError('');
          }}
        />
        {fromField('late-pct', 'Late percent')}
        <input
          data-testid="setting-policy-date"
          placeholder="Rounding policy date, yyyy-MM-dd (blank: the sheet's today)"
          value={policyDate}
          onFocus={() => setTouched((t) => ({ ...t, policyDate: true }))}
          onChange={(e) => {
            setPolicyDate(e.target.value);
            setTouched((t) => ({ ...t, policyDate: true }));
            setError('');
          }}
        />
        <input
          data-testid="setting-period-numbering-from"
          placeholder="Period numbering from, yyyy-MM-dd (blank: off)"
          value={periodNumberingFrom}
          onFocus={() => setTouched((t) => ({ ...t, periodNumberingFrom: true }))}
          onChange={(e) => {
            setPeriodNumberingFrom(e.target.value);
            setTouched((t) => ({ ...t, periodNumberingFrom: true }));
            setError('');
          }}
        />
        <input
          data-testid="setting-entry-date"
          placeholder="Entry date of a new claim, yyyy-MM-dd (blank: off, the sheet's today)"
          value={entryDate}
          onFocus={() => setTouched((t) => ({ ...t, entryDate: true }))}
          onChange={(e) => {
            setEntryDate(e.target.value);
            setTouched((t) => ({ ...t, entryDate: true }));
            setError('');
          }}
        />
        <input
          data-testid="setting-cap"
          placeholder="Cap on a person's due-total (blank: none)"
          value={cap}
          onFocus={() => setTouched((t) => ({ ...t, cap: true }))}
          onChange={(e) => {
            setCap(e.target.value);
            setTouched((t) => ({ ...t, cap: true }));
            setError('');
          }}
        />
        {fromField('cap', 'Cap')}
        <input
          data-testid="setting-proof-threshold"
          placeholder="Proof threshold on a claim's due (blank: none)"
          value={proofThreshold}
          onFocus={() => setTouched((t) => ({ ...t, proofThreshold: true }))}
          onChange={(e) => {
            setProofThreshold(e.target.value);
            setTouched((t) => ({ ...t, proofThreshold: true }));
            setError('');
          }}
        />
        {fromField('proof-threshold', 'Proof threshold')}
        {GRADES.map((g) => (
          <span key={g}>
            <input
              data-testid={`setting-proof-threshold-${g}`}
              placeholder={`Proof threshold for grade ${g} (blank: the sheet's)`}
              value={gradeProof[g] ?? ''}
              onFocus={() => setTouchedProof((t) => ({ ...t, [g]: true }))}
              onChange={(e) => {
                setGradeProof((v) => ({ ...v, [g]: e.target.value }));
                setTouchedProof((t) => ({ ...t, [g]: true }));
                setError('');
              }}
            />
            {fromField(`proof-threshold-${g}`, `Proof threshold for grade ${g}`)}
          </span>
        ))}
        <input
          data-testid="setting-note-threshold"
          placeholder="Note threshold on a claim's due to open it (blank: none)"
          value={noteThreshold}
          onFocus={() => setTouched((t) => ({ ...t, noteThreshold: true }))}
          onChange={(e) => {
            setNoteThreshold(e.target.value);
            setTouched((t) => ({ ...t, noteThreshold: true }));
            setError('');
          }}
        />
        {fromField('note-threshold', 'Note threshold')}
        <input
          data-testid="setting-grace-days"
          placeholder="Grace days after a period's end before it closes itself (blank: off)"
          value={graceDays}
          onFocus={() => setTouched((t) => ({ ...t, graceDays: true }))}
          onChange={(e) => {
            setGraceDays(e.target.value);
            setTouched((t) => ({ ...t, graceDays: true }));
            setError('');
          }}
        />
        <input
          data-testid="setting-pot"
          placeholder="Pot shared out by each top pool's members (blank: none)"
          value={pot}
          onFocus={() => setTouched((t) => ({ ...t, pot: true }))}
          onChange={(e) => {
            setPot(e.target.value);
            setTouched((t) => ({ ...t, pot: true }));
            setError('');
          }}
        />
        <input
          data-testid="setting-parent"
          placeholder="Parent sheet whose rates a grade with none in force takes (blank: none)"
          value={parent}
          onFocus={() => setTouched((t) => ({ ...t, parent: true }))}
          onChange={(e) => {
            setParent(e.target.value);
            setTouched((t) => ({ ...t, parent: true }));
            setError('');
          }}
        />
        <input
          data-testid="setting-fiscal-start-month"
          placeholder="Fiscal year start month, 1 to 12 (blank: 1)"
          value={fiscalStartMonth}
          onFocus={() => setTouched((t) => ({ ...t, fiscalStartMonth: true }))}
          onChange={(e) => {
            setFiscalStartMonth(e.target.value);
            setTouched((t) => ({ ...t, fiscalStartMonth: true }));
            setError('');
          }}
        />
        <input
          data-testid="setting-alert-threshold"
          placeholder="Alert threshold a person's due-total crossing above is alerted (blank: off)"
          value={alertThreshold}
          onFocus={() => setTouched((t) => ({ ...t, alertThreshold: true }))}
          onChange={(e) => {
            setAlertThreshold(e.target.value);
            setTouched((t) => ({ ...t, alertThreshold: true }));
            setError('');
          }}
        />
        {PERMISSION_ACTIONS.map((a) => (
          <input
            key={a}
            data-testid={`setting-perm-${a}`}
            placeholder={`Roles allowed to ${a}, comma separated (blank: ${STANDING_PERMISSIONS[a]})`}
            value={perms[a] ?? ''}
            onFocus={() => setTouchedPerms((t) => ({ ...t, [a]: true }))}
            onChange={(e) => {
              setPerms((v) => ({ ...v, [a]: e.target.value }));
              setTouchedPerms((t) => ({ ...t, [a]: true }));
              setError('');
            }}
          />
        ))}
        <button type="submit" data-testid="settings-save" disabled={save.isPending}>
          Save settings
        </button>
        {error && <span data-testid="settings-error">{error}</span>}
      </form>
    </section>
  );
}
