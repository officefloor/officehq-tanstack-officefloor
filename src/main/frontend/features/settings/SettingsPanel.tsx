import { useRef, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { GRADES } from '../people/usePeople';
import { PERM_ACTIONS, settingsKey, useSettings, type Settings } from './useSettings';

// The sheet's settings: the clerk limit (a clerk settles only claims whose due is at or below it;
// blank: no limit), the floor (a claim's live due is at least it; saved when its field is filled —
// a blank floor field leaves it as it is and saves the clerk limit), the terms (whole days; a claim's
// due-by is its date plus them, fixed when the claim is created; saved when its field is filled, together with any other field filled),
// each grade's own terms (whole days; a new claim whose person's grade on its date has them uses them instead; saved like the terms; none: the sheet's terms), the late percent
// (two decimals; an overdue claim gains it of its figure for every full 30 days overdue; saved like the terms), the policy date (a claim dated
// before it rounds its live figures half up, one on or after it half to even; blank: the sheet's today; saved when its field is
// filled or was the last one entered, together with any other field filled), the period numbering from (from that date,
// a claim opened is numbered within its period; blank: off; saved like the policy date), the cap (two decimals; a person's
// due-total is at most it; blank: none; saved like the policy date) and the role each user name holds (clerk unless set to steward).
// The clerk limit, the floor, the terms, each grade's terms, the late percent and the cap are dated: each has a from-date
// field (blank: the sheet's today) and is saved as its value from that date, keeping the values from other dates; each
// shows the value in force on the sheet's today. The proof threshold (two decimals; blank: none; saved like the cap) is dated
// too: a claim whose due is above the one in force on its date needs a proof to be settled or approved. A claim takes the floor, late percent and terms in force on its date.
// The permission matrix (not dated): for each of open, settle, void, reassign, close and reopen, the roles that may take it,
// comma separated (blank: the standing rule — open, settle, void and close by clerk and steward, reassign and reopen by steward);
// saved like the cap.
export function SettingsPanel() {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const settings = useSettings(sheet);
  const queryClient = useQueryClient();
  const submitSetting = useSubmitOnce();
  const submitRole = useSubmitOnce();
  const [limit, setLimit] = useState('');
  const [floor, setFloor] = useState('');
  const [terms, setTerms] = useState('');
  // Each grade's own terms, by grade, as typed.
  const [gradeTerms, setGradeTerms] = useState<Record<string, string>>({});
  const [latePct, setLatePct] = useState('');
  const [policyDate, setPolicyDate] = useState('');
  // The policy date may be saved blank (back to the sheet's today), so entering its field marks it as the one to save.
  const policyChosen = useRef(false);
  const [numberingFrom, setNumberingFrom] = useState('');
  // Likewise the period numbering from may be saved blank (off).
  const numberingChosen = useRef(false);
  const [cap, setCap] = useState('');
  // Likewise the cap may be saved blank (none).
  const capChosen = useRef(false);
  const [proofThreshold, setProofThreshold] = useState('');
  // Likewise the proof threshold may be saved blank (none).
  const proofChosen = useRef(false);
  // Each action's permission, by action, as typed; each may be saved blank (the standing rule).
  const [perms, setPerms] = useState<Record<string, string>>({});
  const permsChosen = useRef(new Set<string>());
  // Entering another field leaves a blank permission as it is.
  const unchoosePerms = (except?: string) => {
    for (const a of PERM_ACTIONS) {
      if (a !== except && (perms[a] ?? '').trim() === '') permsChosen.current.delete(a);
    }
  };
  // Entering a permission marks it as one to save, as it is.
  const focusPerm = (action: string) => {
    if (policyDate.trim() === '') policyChosen.current = false;
    if (numberingFrom.trim() === '') numberingChosen.current = false;
    if (cap.trim() === '') capChosen.current = false;
    if (proofThreshold.trim() === '') proofChosen.current = false;
    unchoosePerms(action);
    permsChosen.current.add(action);
  };
  // Entering another field leaves a blank policy date (and a blank period numbering from, and a blank cap) as it is.
  const unchoosePolicy = () => {
    unchoosePerms();
    if (policyDate.trim() === '') policyChosen.current = false;
    if (numberingFrom.trim() === '') numberingChosen.current = false;
    if (cap.trim() === '') capChosen.current = false;
    if (proofThreshold.trim() === '') proofChosen.current = false;
  };
  // Each dated setting's from-date, by key, as typed.
  const [froms, setFroms] = useState<Record<string, string>>({});
  const fromField = (key: string, onFocus: () => void = unchoosePolicy) => (
    <input
      data-testid={`setting-${key}-from`}
      onFocus={onFocus}
      value={froms[key] ?? ''}
      placeholder="From YYYY-MM-DD (blank: the sheet's today)"
      onChange={(e) => {
        const value = e.target.value;
        setFroms((f) => ({ ...f, [key]: value }));
        saveSetting.reset();
      }}
    />
  );
  // Entering the cap (or its from-date) marks it as the one to save, as it is.
  const focusCap = () => {
    unchoosePerms();
    if (policyDate.trim() === '') policyChosen.current = false;
    if (numberingFrom.trim() === '') numberingChosen.current = false;
    if (proofThreshold.trim() === '') proofChosen.current = false;
    capChosen.current = true;
  };
  // Entering the proof threshold (or its from-date) marks it as the one to save, as it is.
  const focusProof = () => {
    unchoosePerms();
    if (policyDate.trim() === '') policyChosen.current = false;
    if (numberingFrom.trim() === '') numberingChosen.current = false;
    if (cap.trim() === '') capChosen.current = false;
    proofChosen.current = true;
  };
  const [name, setName] = useState('');
  const [role, setRole] = useState('');
  const saveSetting = useMutation({
    mutationFn: (body: { key: string; value: string; from?: string }) =>
      postJson<Settings>(`/api/sheets/${encodeURIComponent(sheet)}/settings`, { ...body, user }),
    onSuccess: (saved, body) => {
      setFroms((f) => ({ ...f, [body.key]: '' }));
      if (body.key === 'floor') setFloor('');
      else if (body.key === 'terms') setTerms('');
      else if (body.key.startsWith('terms-')) setGradeTerms((t) => ({ ...t, [body.key.slice('terms-'.length)]: '' }));
      else if (body.key === 'late-pct') setLatePct('');
      else if (body.key === 'policy-date') {
        setPolicyDate('');
        policyChosen.current = false;
      }
      else if (body.key === 'period-numbering-from') {
        setNumberingFrom('');
        numberingChosen.current = false;
      }
      else if (body.key === 'cap') {
        setCap('');
        capChosen.current = false;
      }
      else if (body.key.startsWith('perm-')) {
        const action = body.key.slice('perm-'.length);
        setPerms((p) => ({ ...p, [action]: '' }));
        permsChosen.current.delete(action);
      }
      else if (body.key === 'proof-threshold') {
        setProofThreshold('');
        proofChosen.current = false;
      }
      else setLimit('');
      queryClient.setQueryData(settingsKey(sheet), saved);
    },
  });
  const saveRole = useMutation({
    mutationFn: (body: { name: string; role: string }) =>
      postJson<Settings>(`/api/sheets/${encodeURIComponent(sheet)}/roles`, { ...body, user }),
    onSuccess: (saved) => {
      setName('');
      setRole('');
      queryClient.setQueryData(settingsKey(sheet), saved);
    },
  });
  return (
    <>
      <form
        data-testid="settings-form"
        onSubmit={(e) => {
          e.preventDefault();
          submitSetting(() => {
            saveSetting.reset();
            // A dated setting is saved from its from-date (blank: the sheet's today).
            const save = (body: { key: string; value: string }) =>
              saveSetting.mutateAsync({ ...body, from: (froms[body.key] ?? '').trim() });
            const policy = policyDate.trim() !== '' || policyChosen.current;
            const numbering = numberingFrom.trim() !== '' || numberingChosen.current;
            const capped = cap.trim() !== '' || capChosen.current;
            const proofed = proofThreshold.trim() !== '' || proofChosen.current;
            const ownTerms = GRADES.filter((g) => (gradeTerms[g] ?? '').trim() !== '');
            const permitted = PERM_ACTIONS.filter((a) => (perms[a] ?? '').trim() !== '' || permsChosen.current.has(a));
            if (terms.trim() !== '' || ownTerms.length > 0 || latePct.trim() !== '' || policy || numbering || capped || proofed
                || permitted.length > 0) {
              const saves = [
                ...(limit.trim() !== '' ? [{ key: 'clerk-limit', value: limit.trim() }] : []),
                ...(floor.trim() !== '' ? [{ key: 'floor', value: floor.trim() }] : []),
                ...(terms.trim() !== '' ? [{ key: 'terms', value: terms.trim() }] : []),
                ...ownTerms.map((g) => ({ key: `terms-${g}`, value: gradeTerms[g].trim() })),
                ...(latePct.trim() !== '' ? [{ key: 'late-pct', value: latePct.trim() }] : []),
                ...(policy ? [{ key: 'policy-date', value: policyDate.trim() }] : []),
                ...(numbering ? [{ key: 'period-numbering-from', value: numberingFrom.trim() }] : []),
                ...(capped ? [{ key: 'cap', value: cap.trim() }] : []),
                ...(proofed ? [{ key: 'proof-threshold', value: proofThreshold.trim() }] : []),
                ...permitted.map((a) => ({ key: `perm-${a}`, value: (perms[a] ?? '').trim() })),
              ];
              return saves.reduce<Promise<unknown>>((done, body) => done.then(() => save(body)), Promise.resolve());
            }
            if (floor.trim() === '') return save({ key: 'clerk-limit', value: limit.trim() });
            const saveFloor = () => save({ key: 'floor', value: floor.trim() });
            return limit.trim() === ''
              ? saveFloor()
              : save({ key: 'clerk-limit', value: limit.trim() }).then(saveFloor);
          });
        }}
      >
        Clerk limit: <span data-testid="setting-clerk-limit-current">{settings?.clerkLimit ?? ''}</span>{' '}
        <input
          data-testid="setting-clerk-limit"
          onFocus={unchoosePolicy}
          value={limit}
          placeholder="Clerk limit (blank: none)"
          onChange={(e) => {
            setLimit(e.target.value);
            saveSetting.reset();
          }}
        />{' '}
        {fromField('clerk-limit')}{' '}
        Floor: <span data-testid="setting-floor-current">{settings?.floor ?? ''}</span>{' '}
        <input
          data-testid="setting-floor"
          onFocus={unchoosePolicy}
          value={floor}
          placeholder="Floor (blank: unchanged)"
          onChange={(e) => {
            setFloor(e.target.value);
            saveSetting.reset();
          }}
        />{' '}
        {fromField('floor')}{' '}
        Terms (days): <span data-testid="setting-terms-current">{settings?.terms ?? ''}</span>{' '}
        <input
          data-testid="setting-terms"
          onFocus={unchoosePolicy}
          value={terms}
          placeholder="Terms in days (blank: unchanged)"
          onChange={(e) => {
            setTerms(e.target.value);
            saveSetting.reset();
          }}
        />{' '}
        {fromField('terms')}{' '}
        {GRADES.map((g) => (
          <span key={g}>
            Terms for grade {g} (days): <span data-testid={`setting-terms-${g}-current`}>{settings?.gradeTerms?.[g] ?? ''}</span>{' '}
            <input
              data-testid={`setting-terms-${g}`}
              onFocus={unchoosePolicy}
              value={gradeTerms[g] ?? ''}
              placeholder={`Grade ${g} terms in days (blank: unchanged)`}
              onChange={(e) => {
                const value = e.target.value;
                setGradeTerms((t) => ({ ...t, [g]: value }));
                saveSetting.reset();
              }}
            />{' '}
            {fromField(`terms-${g}`)}{' '}
          </span>
        ))}
        Late percent: <span data-testid="setting-late-pct-current">{settings?.latePct ?? ''}</span>{' '}
        <input
          data-testid="setting-late-pct"
          onFocus={unchoosePolicy}
          value={latePct}
          placeholder="Late percent (blank: unchanged)"
          onChange={(e) => {
            setLatePct(e.target.value);
            saveSetting.reset();
          }}
        />{' '}
        {fromField('late-pct')}{' '}
        Policy date: <span data-testid="setting-policy-date-current">{settings?.policyDate ?? ''}</span>{' '}
        <input
          data-testid="setting-policy-date"
          value={policyDate}
          placeholder="Policy date YYYY-MM-DD (blank: the sheet's today)"
          onFocus={() => {
            unchoosePerms();
            if (numberingFrom.trim() === '') numberingChosen.current = false;
            if (cap.trim() === '') capChosen.current = false;
            if (proofThreshold.trim() === '') proofChosen.current = false;
            policyChosen.current = true;
          }}
          onChange={(e) => {
            setPolicyDate(e.target.value);
            policyChosen.current = true;
            saveSetting.reset();
          }}
        />{' '}
        Period numbering from: <span data-testid="setting-period-numbering-from-current">{settings?.periodNumberingFrom ?? ''}</span>{' '}
        <input
          data-testid="setting-period-numbering-from"
          value={numberingFrom}
          placeholder="Period numbering from YYYY-MM-DD (blank: off)"
          onFocus={() => {
            unchoosePerms();
            if (policyDate.trim() === '') policyChosen.current = false;
            if (cap.trim() === '') capChosen.current = false;
            if (proofThreshold.trim() === '') proofChosen.current = false;
            numberingChosen.current = true;
          }}
          onChange={(e) => {
            setNumberingFrom(e.target.value);
            numberingChosen.current = true;
            saveSetting.reset();
          }}
        />{' '}
        Cap: <span data-testid="setting-cap-current">{settings?.cap ?? ''}</span>{' '}
        <input
          data-testid="setting-cap"
          value={cap}
          placeholder="Cap (blank: none)"
          onFocus={focusCap}
          onChange={(e) => {
            setCap(e.target.value);
            capChosen.current = true;
            saveSetting.reset();
          }}
        />{' '}
        {fromField('cap', focusCap)}{' '}
        Proof threshold: <span data-testid="setting-proof-threshold-current">{settings?.proofThreshold ?? ''}</span>{' '}
        <input
          data-testid="setting-proof-threshold"
          value={proofThreshold}
          placeholder="Proof threshold (blank: none)"
          onFocus={focusProof}
          onChange={(e) => {
            setProofThreshold(e.target.value);
            proofChosen.current = true;
            saveSetting.reset();
          }}
        />{' '}
        {fromField('proof-threshold', focusProof)}{' '}
        {PERM_ACTIONS.map((a) => (
          <span key={a}>
            May {a}: <span data-testid={`setting-perm-${a}-current`}>{settings?.perms?.[a] ?? ''}</span>{' '}
            <input
              data-testid={`setting-perm-${a}`}
              value={perms[a] ?? ''}
              placeholder={`Roles that may ${a}, comma separated (blank: the standing rule)`}
              onFocus={() => focusPerm(a)}
              onChange={(e) => {
                const value = e.target.value;
                setPerms((p) => ({ ...p, [a]: value }));
                permsChosen.current.add(a);
                saveSetting.reset();
              }}
            />{' '}
          </span>
        ))}
        <button type="submit" data-testid="settings-save">Save settings</button>
        {saveSetting.isError && <span data-testid="settings-error" role="alert"> Could not save: the clerk limit must be an amount of 0.00 or more with at most two decimals, or blank for none; the floor must be an amount of 0.00 or more with at most two decimals; the terms (and a grade's terms) must be a whole number of days, 0 or more; the late percent must be a percent of 0.00 or more with at most two decimals; the policy date must be a date (YYYY-MM-DD), or blank for the sheet's today; the period numbering from must be a date (YYYY-MM-DD), or blank for off; the cap must be an amount of 0.00 or more with at most two decimals, or blank for none; the proof threshold must be an amount of 0.00 or more with at most two decimals, or blank for none; a permission must be a comma separated list of the roles clerk and steward, or blank for the standing rule; a from-date must be a date (YYYY-MM-DD), or blank for the sheet's today.</span>}
      </form>
      <table data-testid="role-list">
        <thead>
          <tr>
            <th>User</th>
            <th>Role</th>
          </tr>
        </thead>
        <tbody>
          {(settings?.roles ?? []).map((r) => (
            <tr key={r.user} data-testid={`role-row-${r.user}`}>
              <td data-testid="role-user">{r.user}</td>
              <td data-testid="role-value">{r.role}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <form
        data-testid="role-form"
        onSubmit={(e) => {
          e.preventDefault();
          submitRole(() => {
            saveRole.reset();
            return saveRole.mutateAsync({ name: name.trim(), role: role.trim() });
          });
        }}
      >
        <input
          data-testid="role-form-user"
          value={name}
          placeholder="User name"
          onChange={(e) => {
            setName(e.target.value);
            saveRole.reset();
          }}
        />{' '}
        <input
          data-testid="role-form-role"
          value={role}
          placeholder="Role (clerk or steward)"
          onChange={(e) => {
            setRole(e.target.value);
            saveRole.reset();
          }}
        />{' '}
        <button type="submit" data-testid="role-form-submit">Set role</button>
        {saveRole.isError && <span data-testid="role-form-error" role="alert"> Could not set the role: it needs a user name and the role clerk or steward.</span>}
      </form>
    </>
  );
}
