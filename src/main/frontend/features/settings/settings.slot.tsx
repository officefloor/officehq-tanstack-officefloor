import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { byUser } from '../session/session';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { GRADE_TERMS, isDated, settingsKey, settingsPath, useSettings } from './settings';
import type { SettingKey, Settings as SheetSettings } from './settings';

// The sheet's settings: each value in force, and a form that changes it. The clerk limit is the most due
// a clerk may settle (two decimals); blank means no limit. The floor is the least a claim's live due is (two
// decimals); blank takes its default, 1.00. The terms are the whole days after its date by which a claim is due;
// blank takes its default, 14. The late percent (two decimals) is charged on an overdue claim's figure for every
// full 30 days it is overdue; blank takes its default, 0.00. A claim dated before the policy date rounds half up, on
// or after it half to even; blank makes it the sheet's today. While the sheet's today is on or after the period
// numbering from-date, a claim opened is numbered within its period; blank turns that off. A grade's own terms (whole
// days) fix the due-by of a claim added for a person of that grade; blank makes the grade take the sheet's terms. Saving sends each setting whose field was changed; with none changed it
// sends the clerk limit, so saving a blank form clears the limit. The cap is the most a person's due-total is (two
// decimals); blank means no cap. Each setting that carries a figure (all but the policy date and the period numbering
// from-date) has a from-date beside it: its value is recorded from that date (blank: the sheet's today), keeping the
// values recorded from other dates, and each shows the value in force on the sheet's today.
function Settings({ sheet, user }: { sheet: string; user: string }) {
  const queryClient = useQueryClient();
  const { data } = useSettings(sheet);
  const [clerkLimit, setClerkLimit] = useState('');
  const [cap, setCap] = useState('');
  const [floor, setFloor] = useState('');
  const [terms, setTerms] = useState('');
  const [latePct, setLatePct] = useState('');
  const [policyDate, setPolicyDate] = useState('');
  const [periodNumberingFrom, setPeriodNumberingFrom] = useState('');
  const [gradeTerms, setGradeTerms] = useState<Record<string, string>>({});
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
    onSuccess: () => {
      setClerkLimit('');
      setCap('');
      setFloor('');
      setTerms('');
      setLatePct('');
      setPolicyDate('');
      setPeriodNumberingFrom('');
      setGradeTerms({});
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
    const values: Record<SettingKey, string> = { 'clerk-limit': clerkLimit, floor, terms, 'late-pct': latePct, 'policy-date': policyDate,
      'period-numbering-from': periodNumberingFrom, 'terms-A': gradeTerms.A ?? '', 'terms-B': gradeTerms.B ?? '',
      'terms-C': gradeTerms.C ?? '', cap };
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
        now <span data-testid="setting-clerk-limit-current">{data?.clerkLimit ?? ''}</span>{' '}
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
        now <span data-testid="setting-cap-current">{data?.cap ?? ''}</span>{' '}
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
        now <span data-testid="setting-floor-current">{data?.floor ?? ''}</span>{' '}
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
        now <span data-testid="setting-terms-current">{data?.terms ?? ''}</span>{' '}
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
            now <span data-testid={`setting-${key}-current`}>{data?.[field] ?? ''}</span>{' '}
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
        now <span data-testid="setting-late-pct-current">{data?.latePct ?? ''}</span>{' '}
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
        now <span data-testid="setting-policy-date-current">{data?.policyDate ?? ''}</span>{' '}
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
        now <span data-testid="setting-period-numbering-from-current">{data?.periodNumberingFrom ?? ''}</span>{' '}
        <button type="submit" data-testid="settings-save" disabled={save.isPending}>
          Save settings
        </button>
        {save.isError && (
          <span data-testid="settings-error" role="alert">
            {' '}The clerk limit, the cap, the floor and the late percent must be 0.00 or more with up to two decimals, the terms (the sheet's and each grade's) a whole number of days, the policy date, the period numbering from-date and each from-date dates, or blank.
          </span>
        )}
      </form>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 6, Component: Settings });
