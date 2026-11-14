import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useOneAtATime } from '../../query/useOneAtATime';
import { peopleKey } from '../people/people';
import type { Person } from '../people/people';
import { usePersonError } from '../people/personError';
import { useCurrentSheet, useCurrentUser } from '../session/session';

// The person's approver (a user who may approve or reject their PENDING claims as a steward may), and the user name
// (blank: none) and button that set it.
export function PersonApprover({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [approver, setApprover] = useState('');
  const [, setError] = usePersonError(person.id);
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const save = useMutation({
    mutationFn: (body: { user: string }) =>
      postJson<Person>(
        `/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/approver?user=${encodeURIComponent(user)}`,
        body,
      ),
    onSuccess: async () => {
      setApprover('');
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheet) });
    },
    onError: () => setError('The approver could not be set.'),
  });

  const submit = () => {
    setError('');
    const a = approver.trim();
    if (/\s/.test(a)) {
      setError('An approver is a single user name.');
      return;
    }
    once(() => save.mutateAsync({ user: a }));
  };

  return (
    <td>
      <span data-testid="person-approver">{person.approver ?? ''}</span>{' '}
      <input
        data-testid="person-approver-input"
        placeholder="Approver"
        value={approver}
        onChange={(e) => {
          setApprover(e.target.value);
          setError('');
        }}
      />
      <button type="button" data-testid="person-approver-save" disabled={save.isPending} onClick={submit}>
        Set approver
      </button>
    </td>
  );
}
