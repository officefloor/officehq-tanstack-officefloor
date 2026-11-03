import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PersonCells, PersonColumnHead } from '../../slots/defs/personRow';
import { peopleKey, usePeople, type Person } from '../people/usePeople';
import { usePersonRowAction } from '../people/usePersonRowAction';

/** A person's approver: the user name shown, and an entry and a button by which it is set (blank: none). The approver
 *  may approve or reject the person's PENDING claims as a steward may. */
function PersonApprover({ sheetId, personId }: { sheetId: string; personId: number }) {
  const people = usePeople(sheetId);
  const queryClient = useQueryClient();
  const p = people.data?.find((x) => x.id === personId);
  const [user, setUser] = useState('');

  const save = useMutation({
    mutationFn: (body: { user: string }) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/approver`, body),
    onSuccess: async () => {
      setUser('');
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
    },
  });
  const announce = usePersonRowAction(sheetId, personId, save.reset);

  return (
    <>
      <td data-testid="person-approver">{p?.approver ?? ''}</td>
      <td>
        <input
          type="text"
          placeholder="approver"
          data-testid="person-approver-input"
          value={user}
          onChange={(e) => {
            setUser(e.target.value);
            save.reset();
          }}
        />{' '}
        <button
          type="button"
          data-testid="person-approver-save"
          disabled={save.isPending}
          onClick={() => {
            announce();
            save.reset();
            save.mutate({ user: user.trim() });
          }}
        >
          Set approver
        </button>
        {save.isError && <span data-testid="person-error">{save.error.message}</span>}
      </td>
    </>
  );
}

export const contributions = [
  PersonColumnHead.fill({
    order: 47,
    Component: () => (
      <>
        <th>Approver</th>
        <th />
      </>
    ),
  }),
  PersonCells.fill({ order: 47, Component: PersonApprover }),
];
