import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { PersonRow } from '../../slots/defs/personRow';
import { usePersonError } from '../people/rowError';
import type { Person } from '../people/people';
import { flagsKey, useFlags } from './flags';
import type { Flag } from './flags';

/** The reason the person is flagged for while the sheet's today is on or before the flag's until date (blank
 *  otherwise), and the form a steward flags them with. While flagged, only a steward may open their claims. */
function PersonFlag({ sheetId, user, person }: { sheetId: string; user: string; person: Person }) {
  const queryClient = useQueryClient();
  const flag = (useFlags(sheetId).data ?? []).find((f) => f.personId === person.id);
  const [reason, setReason] = useState('');
  const [until, setUntil] = useState('');
  const { error, setError, begin } = usePersonError(sheetId, person.id);
  const once = useSubmitOnce();

  const save = useMutation({
    mutationFn: (body: { user: string; reason: string; until: string }) =>
      putJson<Flag>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/flag`, body),
    // The row shows the flag just saved, as the server answered it; a reload shows only the flags in force.
    onSuccess: (saved) => {
      queryClient.setQueryData<Flag[]>(flagsKey(sheetId), (old) =>
        [...(old ?? []).filter((f) => f.personId !== saved.personId), saved]);
      setReason('');
      setUntil('');
    },
    onError: (e) =>
      setError(e instanceof HttpError && e.body ? e.body : `Could not flag ${person.name}: give a reason and a yyyy-mm-dd date.`),
    onSettled: once.done,
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    begin();
    if (!once.start()) return;
    save.mutate({ user, reason: reason.trim(), until: until.trim() });
  };

  const change = (set: (v: string) => void) => (e: { target: { value: string } }) => {
    set(e.target.value);
    setError('');
  };

  return (
    <td>
      Flag <span data-testid="person-flag-shown">{flag?.reason ?? ''}</span>
      {flag && <> until {flag.until}</>}{' '}
      <form onSubmit={submit} style={{ display: 'inline' }}>
        <input data-testid="person-flag-reason" placeholder="reason" value={reason} onChange={change(setReason)} />{' '}
        <input data-testid="person-flag-until" placeholder="until yyyy-mm-dd" value={until} onChange={change(setUntil)} />{' '}
        <button type="submit" data-testid="person-flag" disabled={save.isPending}>
          Flag
        </button>
      </form>
      {error && (
        <p data-testid="person-error" role="alert">
          {error}
        </p>
      )}
    </td>
  );
}

export const contribution = PersonRow.fill({ order: 46, Component: PersonFlag });
