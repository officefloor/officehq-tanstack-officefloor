import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { settingsKey, useSettings, type Settings } from './useSettings';

// The sheet's settings: the clerk limit (a clerk settles only claims whose due is at or below it;
// blank: no limit), the floor (a claim's live due is at least it; saved when its field is filled —
// a blank floor field leaves it as it is and saves the clerk limit), the terms (whole days; a claim's
// due-by is its date plus them; saved when its field is filled, together with any other field filled), the late percent
// (two decimals; an overdue claim gains it of its figure for every full 30 days overdue; saved like the terms) and the role each user name holds (clerk unless set to steward).
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
  const [latePct, setLatePct] = useState('');
  const [name, setName] = useState('');
  const [role, setRole] = useState('');
  const saveSetting = useMutation({
    mutationFn: (body: { key: string; value: string }) =>
      postJson<Settings>(`/api/sheets/${encodeURIComponent(sheet)}/settings`, { ...body, user }),
    onSuccess: (saved, body) => {
      if (body.key === 'floor') setFloor('');
      else if (body.key === 'terms') setTerms('');
      else if (body.key === 'late-pct') setLatePct('');
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
            if (terms.trim() !== '' || latePct.trim() !== '') {
              const saves = [
                ...(limit.trim() !== '' ? [{ key: 'clerk-limit', value: limit.trim() }] : []),
                ...(floor.trim() !== '' ? [{ key: 'floor', value: floor.trim() }] : []),
                ...(terms.trim() !== '' ? [{ key: 'terms', value: terms.trim() }] : []),
                ...(latePct.trim() !== '' ? [{ key: 'late-pct', value: latePct.trim() }] : []),
              ];
              return saves.reduce<Promise<unknown>>((done, body) => done.then(() => saveSetting.mutateAsync(body)), Promise.resolve());
            }
            if (floor.trim() === '') return saveSetting.mutateAsync({ key: 'clerk-limit', value: limit.trim() });
            const saveFloor = () => saveSetting.mutateAsync({ key: 'floor', value: floor.trim() });
            return limit.trim() === ''
              ? saveFloor()
              : saveSetting.mutateAsync({ key: 'clerk-limit', value: limit.trim() }).then(saveFloor);
          });
        }}
      >
        Clerk limit: <span data-testid="setting-clerk-limit-current">{settings?.clerkLimit ?? ''}</span>{' '}
        <input
          data-testid="setting-clerk-limit"
          value={limit}
          placeholder="Clerk limit (blank: none)"
          onChange={(e) => {
            setLimit(e.target.value);
            saveSetting.reset();
          }}
        />{' '}
        Floor: <span data-testid="setting-floor-current">{settings?.floor ?? ''}</span>{' '}
        <input
          data-testid="setting-floor"
          value={floor}
          placeholder="Floor (blank: unchanged)"
          onChange={(e) => {
            setFloor(e.target.value);
            saveSetting.reset();
          }}
        />{' '}
        Terms (days): <span data-testid="setting-terms-current">{settings?.terms ?? ''}</span>{' '}
        <input
          data-testid="setting-terms"
          value={terms}
          placeholder="Terms in days (blank: unchanged)"
          onChange={(e) => {
            setTerms(e.target.value);
            saveSetting.reset();
          }}
        />{' '}
        Late percent: <span data-testid="setting-late-pct-current">{settings?.latePct ?? ''}</span>{' '}
        <input
          data-testid="setting-late-pct"
          value={latePct}
          placeholder="Late percent (blank: unchanged)"
          onChange={(e) => {
            setLatePct(e.target.value);
            saveSetting.reset();
          }}
        />{' '}
        <button type="submit" data-testid="settings-save">Save settings</button>
        {saveSetting.isError && <span data-testid="settings-error" role="alert"> Could not save: the clerk limit must be an amount of 0.00 or more with at most two decimals, or blank for none; the floor must be an amount of 0.00 or more with at most two decimals; the terms must be a whole number of days, 0 or more; the late percent must be a percent of 0.00 or more with at most two decimals.</span>}
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
