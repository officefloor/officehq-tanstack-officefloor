import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { PersonRow } from '../../slots/defs/personRow';
import { usePersonError } from '../people/rowError';
import type { Person } from '../people/people';
import { approversKey, useApprovers } from './approvers';
import type { Approver } from './approvers';

/** The person's approver (blank when none), and the form that sets it. The approver may approve or reject the
 *  person's PENDING claims as a steward may, though not one they opened or last edited. A blank name clears it. */
function PersonApprover({ sheetId, user, person }: { sheetId: string; user: string; person: Person }) {
  const queryClient = useQueryClient();
  const approver = (useApprovers(sheetId).data ?? []).find((a) => a.personId === person.id);
  const [name, setName] = useState('');
  const { error, setError, begin } = usePersonError(sheetId, person.id);
  const once = useSubmitOnce();

  const save = useMutation({
    mutationFn: (body: { user: string; approver: string }) =>
      putJson<Approver>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/approver`, body),
    onSuccess: (saved) => {
      queryClient.setQueryData<Approver[]>(approversKey(sheetId), (old) => [
        ...(old ?? []).filter((a) => a.personId !== saved.personId),
        ...(saved.approver ? [saved] : []),
      ]);
      setName('');
    },
    onError: (e) =>
      setError(e instanceof HttpError && e.body ? e.body : `Could not set ${person.name}'s approver: give one user name.`),
    onSettled: once.done,
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    begin();
    if (!once.start()) return;
    save.mutate({ user, approver: name.trim() });
  };

  return (
    <td>
      Approver <span data-testid="person-approver">{approver?.approver ?? ''}</span>{' '}
      <form onSubmit={submit} style={{ display: 'inline' }}>
        <input
          data-testid="person-approver-input"
          placeholder="approver"
          value={name}
          onChange={(e) => {
            setName(e.target.value);
            setError('');
          }}
        />{' '}
        <button type="submit" data-testid="person-approver-save" disabled={save.isPending}>
          Set approver
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

export const contribution = PersonRow.fill({ order: 46.5, Component: PersonApprover });
