import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PersonCells, PersonColumns } from '../../slots/defs/personRow';
import { peopleKey, type Person } from '../people/usePeople';
import { useRowAction } from '../people/useRowAction';

/**
 * A person's approver: the user shown, and the entry and button to set it (blank: none). The approver
 * may approve or reject the person's PENDING claims as a steward may, but not one they opened or last edited.
 */
function PersonApprover({ user, sheet, person }: { user: string; sheet: string; person: Person }) {
  const queryClient = useQueryClient();
  const [approver, setApprover] = useState('');
  const save = useMutation({
    mutationFn: (a: string) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/approver`, { approver: a, user }),
    onSuccess: () => setApprover(''),
    onSettled: () => queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
  });
  const actionTaken = useRowAction(sheet, person.id, save.reset);
  return (
    <>
      <td data-testid="person-approver">{person.approver ?? ''}</td>
      <td>
        <input
          type="text"
          placeholder="approver"
          data-testid="person-approver-input"
          value={approver}
          onChange={(e) => {
            setApprover(e.target.value);
            save.reset();
          }}
        />{' '}
        <button
          type="button"
          data-testid="person-approver-save"
          disabled={save.isPending}
          onClick={() => {
            actionTaken();
            save.mutate(approver);
          }}
        >
          Set approver
        </button>
        {save.isError && (
          <span data-testid="person-error" role="alert">
            {' '}
            Could not set the approver.
          </span>
        )}
      </td>
    </>
  );
}

export const contributions = [
  PersonColumns.fill({
    order: 36.6,
    Component: () => (
      <>
        <th>Approver</th>
        <th />
      </>
    ),
  }),
  PersonCells.fill({ order: 36.6, Component: PersonApprover }),
];
