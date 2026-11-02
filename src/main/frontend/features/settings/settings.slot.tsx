import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSingleFlight } from '../../query/useSingleFlight';
import { SheetBody } from '../../slots/defs/sheetBody';
import { settingsKey, useSettings, type Settings } from './useSettings';

/** The sheet's settings: its clerk limit (a clerk settles a claim only when its due is at or below
 *  it; blank is no limit), its floor (the least a live due can be), its terms (the days after
 *  a claim's date that it is due by), its late percent (what an overdue claim's due gains, of its
 *  figure, for every full 30 days it is overdue), its policy date (a claim dated before it rounds half
 *  up, one on or after it half to even; blank is the sheet's today), its period numbering from-date
 *  (while set and the sheet's today is on or after it, a claim opened is numbered within its period,
 *  <period>-<n>, or X-<n> in no period; blank is off), each grade's own terms (blank is the sheet's
 *  terms), and the role (clerk or steward)
 *  assigned to each user name. Saving saves each setting whose field was entered (focused or typed
 *  in, even when left blank); the others stay as they are. */
/** The grades a person can have, each of which can have its own terms. */
const GRADES = ['A', 'B', 'C'];

function SettingsPanel({ sheetId }: { sheetId: string }) {
  const settings = useSettings(sheetId);
  const queryClient = useQueryClient();
  const [limit, setLimit] = useState<string | null>(null);
  const [floor, setFloor] = useState<string | null>(null);
  const [terms, setTerms] = useState<string | null>(null);
  const [gradeTerms, setGradeTerms] = useState<Record<string, string>>({});
  const [latePct, setLatePct] = useState<string | null>(null);
  const [policyDate, setPolicyDate] = useState<string | null>(null);
  const [numberingFrom, setNumberingFrom] = useState<string | null>(null);
  const [user, setUser] = useState('');
  const [role, setRole] = useState('');

  const saveOnce = useSingleFlight();
  const save = useMutation({
    mutationFn: (f: { clerkLimit: string | null; floor: string | null; terms: string | null; latePct: string | null; policyDate: string | null; periodNumberingFrom: string | null; gradeTerms: Record<string, string> }) =>
      postJson<Settings>(`/api/sheets/${encodeURIComponent(sheetId)}/settings`, f),
    onSuccess: (saved) => {
      setLimit(null);
      setFloor(null);
      setTerms(null);
      setGradeTerms({});
      setLatePct(null);
      setPolicyDate(null);
      setNumberingFrom(null);
      queryClient.setQueryData(settingsKey(sheetId), saved);
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

  return (
    <section data-testid="settings">
      <form
        data-testid="settings-form"
        onSubmit={(e) => {
          e.preventDefault();
          saveOnce(() => {
            save.reset();
            return save.mutateAsync({ clerkLimit: limit, floor, terms, latePct, policyDate, periodNumberingFrom: numberingFrom, gradeTerms });
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
        <label>
          Terms (days){' '}
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
        {GRADES.map((g) => (
          <label key={g}>
            Terms {g} (days){' '}
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
        <button type="submit" data-testid="settings-save" disabled={save.isPending}>
          Save settings
        </button>{' '}
        Current: clerk limit <strong data-testid="setting-clerk-limit-current">{settings.data?.clerkLimit ?? ''}</strong>,
        floor <strong data-testid="setting-floor-current">{settings.data?.floor ?? ''}</strong>,
        terms <strong data-testid="setting-terms-current">{settings.data?.terms ?? ''}</strong>,
        {GRADES.map((g) => (
          <span key={g}>
            terms {g} <strong data-testid={`setting-terms-${g}-current`}>{settings.data?.gradeTerms?.[g] ?? ''}</strong>,{' '}
          </span>
        ))}
        late % <strong data-testid="setting-late-pct-current">{settings.data?.latePct ?? ''}</strong>,
        policy date <strong data-testid="setting-policy-date-current">{settings.data?.policyDate ?? ''}</strong>,
        period numbering from{' '}
        <strong data-testid="setting-period-numbering-from-current">{settings.data?.periodNumberingFrom ?? ''}</strong>
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
            placeholder="clerk or steward"
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
      {settings.isError && <p data-testid="settings-load-error">{settings.error.message}</p>}
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 6, Component: ({ sheetId }) => <SettingsPanel sheetId={sheetId} /> });
