import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { PersonHead, PersonRow } from '../../slots/defs/personRow';
import { peopleKey } from '../people/people';
import type { Person } from '../people/people';
import { useUser } from '../session/session';
import { useRowError } from '../people/rowError';

function ApproverHead() {
  return (
    <>
      <th>Approver</th>
      <th />
    </>
  );
}

// The user who may approve or reject the person's PENDING claims as a steward may (not one they
// opened or last edited), and a user name with a button that sets it (blank clears it).
function PersonApprover({ sheetId, person }: { sheetId: string; person: Person }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [approver, setApprover] = useState('');
  const [error, setError] = useRowError(person.id, 'approver');
  const once = useOnce();
  const save = useMutation({
    mutationFn: (body: { user: string }) =>
      postJson<Person>(
        `/api/sheets/${encodeURIComponent(sheetId)}/people/${encodeURIComponent(person.id)}/approver`,
        body,
        user,
      ),
    onSuccess: () => {
      setApprover('');
      return queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
    },
    onError: () => setError('That approver could not be set.'),
  });

  const submit = () => {
    setError('');
    once((done) => save.mutate({ user: approver.trim() }, { onSettled: done }));
  };

  return (
    <>
      <td data-testid="person-approver">{person.approver}</td>
      <td>
        <input
          data-testid="person-approver-input"
          aria-label="Approver"
          placeholder="User"
          value={approver}
          onChange={(e) => {
            setApprover(e.target.value);
            setError('');
          }}
        />{' '}
        <button type="button" data-testid="person-approver-save" onClick={submit} disabled={save.isPending}>
          Set approver
        </button>
        {error && (
          <p role="alert" data-testid="person-error">
            {error}
          </p>
        )}
      </td>
    </>
  );
}

export const contributions = [
  PersonHead.fill({ order: 53, Component: ApproverHead }),
  PersonRow.fill({ order: 53, Component: PersonApprover }),
];
