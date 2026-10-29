import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { PersonCells, PersonHeaders } from '../../slots/defs/personColumns';
import { usePersonError } from '../people/usePersonError';
import { useUser } from '../session/useUser';
import { approversKey, approversQuery } from './approversQuery';
import type { Approver } from './approversQuery';

/**
 * The person's approver, and the control that sets one by user name. The approver approves or rejects the person's
 * PENDING claims as a steward may (the opener and the last editor still may not approve their own).
 */
function PersonApprover({ sheetId, personId }: { sheetId: string; personId: number }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [name, setName] = useState('');
  const [, setError] = usePersonError(personId);
  const once = useSubmitOnce();
  const approver = (useQuery(approversQuery(sheetId)).data ?? []).find((a) => a.personId === personId);
  const save = useMutation({
    mutationFn: (body: { user: string }) =>
      putJson<Approver>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/approver`, body, user),
    onSuccess: async () => {
      setName('');
      await queryClient.invalidateQueries({ queryKey: approversKey(sheetId) });
    },
    onError: () => setError('The approver could not be set.'),
  });

  return (
    <td>
      <span data-testid="person-approver">{approver?.user ?? ''}</span>{' '}
      <input
        data-testid="person-approver-input"
        placeholder="user"
        value={name}
        onChange={(e) => {
          setName(e.target.value);
          setError('');
        }}
      />{' '}
      <button
        type="button"
        data-testid="person-approver-save"
        disabled={save.isPending}
        onClick={() => {
          setError('');
          const u = name.trim();
          if (u === '') {
            setError("Give the approver's user name.");
            return;
          }
          once(() => save.mutateAsync({ user: u }));
        }}
      >
        Set approver
      </button>
    </td>
  );
}

export const contributions = [
  PersonHeaders.fill({ order: 19.3, Component: () => <th>Approver</th> }),
  PersonCells.fill({ order: 19.3, Component: PersonApprover }),
];
