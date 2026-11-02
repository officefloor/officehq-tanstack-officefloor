import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSingleFlight } from '../../query/useSingleFlight';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useSheetToday } from '../today/useSheetToday';
import { settingOn, settingsKey, useSettings, type Settings } from './useSettings';

/** The sheet's settings: its clerk limit (a clerk settles a claim only when its due is at or below
 *  it; blank is no limit), its floor (the least a live due can be), its terms (the days after
 *  a claim's date that it is due by), its late percent (what an overdue claim's due gains, of its
 *  figure, for every full 30 days it is overdue), its policy date (a claim dated before it rounds half
 *  up, one on or after it half to even; blank is the sheet's today), its period numbering from-date
 *  (while set and the sheet's today is on or after it, a claim opened is numbered within its period,
 *  <period>-<n>, or X-<n> in no period; blank is off), each grade's own terms (blank is the sheet's
 *  terms), its cap (the most a person's due-total can be; blank is none), its proof threshold (a claim whose due is
 *  above the one in force on its date cannot be settled or approved without a proof; blank is none), its permission matrix (for each of open, settle, void,
 *  reassign, close and reopen, the roles that may do it, comma separated; blank keeps the standing rule; not dated), its grace days (changing the
 *  sheet's today closes each open period whose to-date plus them is before the new today; blank is off; not dated), and the role (clerk, steward or viewer — a viewer changes nothing)
 *  assigned to each user name, and the delegations of the steward role (a steward delegates it to a user from a
 *  from-date, blank the sheet's today, to a to-date, inclusive; while the sheet's today is inside it the user acts
 *  as a steward). Saving saves each setting whose field was entered (focused or typed
 *  in, even when left blank); the others stay as they are. A setting with a figure (the clerk limit, the
 *  floor, the terms, each grade's terms, the late percent, the cap and the proof threshold) is saved from its from-date, blank
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
  const settings = useSettings(sheetId);
  const queryClient = useQueryClient();
  const [limit, setLimit] = useState<string | null>(null);
  const [floor, setFloor] = useState<string | null>(null);
  const [terms, setTerms] = useState<string | null>(null);
  const [gradeTerms, setGradeTerms] = useState<Record<string, string>>({});
  const [latePct, setLatePct] = useState<string | null>(null);
  const [policyDate, setPolicyDate] = useState<string | null>(null);
  const [numberingFrom, setNumberingFrom] = useState<string | null>(null);
  const [cap, setCap] = useState<string | null>(null);
  const [proofThreshold, setProofThreshold] = useState<string | null>(null);
  const [from, setFrom] = useState<Record<string, string>>({});
  const [permissions, setPermissions] = useState<Record<string, string>>({});
  const [graceDays, setGraceDays] = useState<string | null>(null);
  const today = useSheetToday(sheetId).data?.today;
  const [user, setUser] = useState('');
  const [role, setRole] = useState('');

  const [delegateUser, setDelegateUser] = useState('');
  const [delegateFrom, setDelegateFrom] = useState('');
  const [delegateTo, setDelegateTo] = useState('');

  const saveOnce = useSingleFlight();
  const save = useMutation({
    mutationFn: (f: { clerkLimit: string | null; floor: string | null; terms: string | null; latePct: string | null; policyDate: string | null; periodNumberingFrom: string | null; gradeTerms: Record<string, string>; cap: string | null; from: Record<string, string>; proofThreshold: string | null; permissions: Record<string, string>; graceDays: string | null }) =>
      postJson<Settings>(`/api/sheets/${encodeURIComponent(sheetId)}/settings`, f),
    onSuccess: (saved) => {
      setLimit(null);
      setFloor(null);
      setTerms(null);
      setGradeTerms({});
      setLatePct(null);
      setPolicyDate(null);
      setNumberingFrom(null);
      setCap(null);
      setProofThreshold(null);
      setFrom({});
      setPermissions({});
      setGraceDays(null);
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

  return (
    <section data-testid="settings">
      <form
        data-testid="settings-form"
        onSubmit={(e) => {
          e.preventDefault();
          saveOnce(() => {
            save.reset();
            return save.mutateAsync({ clerkLimit: limit, floor, terms, latePct, policyDate, periodNumberingFrom: numberingFrom, gradeTerms, cap, from, proofThreshold, permissions, graceDays });
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
        <FromInput settingKey="terms" from={from} setFrom={setFrom} onChange={() => save.reset()} />{' '}
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
            <FromInput settingKey={`terms-${g}`} from={from} setFrom={setFrom} onChange={() => save.reset()} />{' '}
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
        <button type="submit" data-testid="settings-save" disabled={save.isPending}>
          Save settings
        </button>{' '}
        Current: clerk limit <strong data-testid="setting-clerk-limit-current">{settingOn(settings.data, 'clerk-limit', today) ?? ''}</strong>,
        floor <strong data-testid="setting-floor-current">{settingOn(settings.data, 'floor', today) ?? ''}</strong>,
        terms <strong data-testid="setting-terms-current">{settingOn(settings.data, 'terms', today) ?? ''}</strong>,
        {GRADES.map((g) => (
          <span key={g}>
            terms {g} <strong data-testid={`setting-terms-${g}-current`}>{settingOn(settings.data, `terms-${g}`, today) ?? ''}</strong>,{' '}
          </span>
        ))}
        late % <strong data-testid="setting-late-pct-current">{settingOn(settings.data, 'late-pct', today) ?? ''}</strong>,
        policy date <strong data-testid="setting-policy-date-current">{settings.data?.policyDate ?? ''}</strong>,
        period numbering from{' '}
        <strong data-testid="setting-period-numbering-from-current">{settings.data?.periodNumberingFrom ?? ''}</strong>,
        cap <strong data-testid="setting-cap-current">{settingOn(settings.data, 'cap', today) ?? ''}</strong>,
        proof threshold <strong data-testid="setting-proof-threshold-current">{settingOn(settings.data, 'proof-threshold', today) ?? ''}</strong>
        {PERM_ACTIONS.map((a) => (
          <span key={a}>
            , may {a} <strong data-testid={`setting-perm-${a}-current`}>{settings.data?.permissions?.[a] ?? ''}</strong>
          </span>
        ))}
        , grace days <strong data-testid="setting-grace-days-current">{settings.data?.graceDays ?? ''}</strong>
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
      {settings.isError && <p data-testid="settings-load-error">{settings.error.message}</p>}
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 6, Component: ({ sheetId }) => <SettingsPanel sheetId={sheetId} /> });
