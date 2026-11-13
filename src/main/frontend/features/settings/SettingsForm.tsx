import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import {
  AMOUNT,
  DATE,
  DAYS,
  DEFAULT_FLOOR,
  DEFAULT_LATE_PCT,
  DEFAULT_TERMS,
  PERCENT,
  settingsKey,
  useSettings,
} from './settings';
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
  cap?: string;
  clerkLimitFrom?: string;
  floorFrom?: string;
  termsFrom?: string;
  latePctFrom?: string;
  capFrom?: string;
  gradeTermsFrom?: Record<string, string>;
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
  cap: boolean;
};

const UNTOUCHED: Touched = {
  clerkLimit: false,
  floor: false,
  terms: false,
  latePct: false,
  policyDate: false,
  periodNumberingFrom: false,
  cap: false,
};

// The sheet's clerk limit (blank: no limit), floor, terms, each grade's terms (blank: the sheet's), late percent and policy date (blank: today) and period numbering from date (blank: off) and cap (blank: none) in force, and the form that sets them. Saving sends
// only the fields touched since the last save, so a blank field left alone changes nothing. A dated setting (the clerk
// limit, floor, terms, each grade's terms, late percent and cap) is recorded from the date typed beside it (blank: the
// sheet's today); the value shown is the one in force on the sheet's today.
export function SettingsForm() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const settings = useSettings().data;
  const [clerkLimit, setClerkLimit] = useState('');
  const [floor, setFloor] = useState('');
  const [terms, setTerms] = useState('');
  const [gradeTerms, setGradeTerms] = useState<Record<string, string>>({});
  const [touchedGrades, setTouchedGrades] = useState<Record<string, boolean>>({});
  const [latePct, setLatePct] = useState('');
  const [policyDate, setPolicyDate] = useState('');
  const [periodNumberingFrom, setPeriodNumberingFrom] = useState('');
  const [cap, setCap] = useState('');
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
      setLatePct('');
      setPolicyDate('');
      setPeriodNumberingFrom('');
      setCap('');
      setFroms({});
      setTouched(UNTOUCHED);
      setError('');
      queryClient.setQueryData(settingsKey(sheet), saved);
      await queryClient.invalidateQueries({ queryKey: settingsKey(sheet) });
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
      ['late-pct', 'late percent'],
      ['cap', 'cap'],
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
    if (touched.cap) {
      const value = cap.trim();
      if (value !== '' && !AMOUNT.test(value)) {
        setError('Type a cap of 0.00 or more, with at most two decimals, or leave it blank for none.');
        return;
      }
      input.cap = value;
      input.capFrom = datedFrom('cap');
    }
    if (
      input.clerkLimit === undefined &&
      input.floor === undefined &&
      input.terms === undefined &&
      input.gradeTerms === undefined &&
      input.latePct === undefined &&
      input.policyDate === undefined &&
      input.periodNumberingFrom === undefined &&
      input.cap === undefined
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
        Cap: <span data-testid="setting-cap-current">{settings?.cap ?? ''}</span>
      </p>
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
        <button type="submit" data-testid="settings-save" disabled={save.isPending}>
          Save settings
        </button>
        {error && <span data-testid="settings-error">{error}</span>}
      </form>
    </section>
  );
}
