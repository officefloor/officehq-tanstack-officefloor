import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { type Person, peopleKey, personActionKey, useLatestPersonAction } from '../people/people';
import { useCurrentSheet, useCurrentUser } from '../../session/session';

function ApproverHead() {
  return <th>Approver</th>;
}

// The person's approver (nothing when none), and the entry and button that set it (a blank one clears it). The
// approver may approve or reject the person's PENDING claims as a steward may. A refused change shows its error
// until the entry is changed or the next is tried.
function PersonApprover({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [approver, setApprover] = useState('');
  const queryClient = useQueryClient();

  const save = useMutation({
    mutationKey: personActionKey(sheet, person.id),
    mutationFn: (a: string) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/approver`, { approver: a, user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheet) });
      setApprover('');
    },
  });

  const latest = useLatestPersonAction(sheet, person.id);

  return (
    <td>
      <span data-testid="person-approver">{person.approver ?? ''}</span>
      <input
        data-testid="person-approver-input"
        aria-label="Approver"
        placeholder="Approver"
        value={approver}
        onChange={(e) => {
          save.reset();
          setApprover(e.target.value);
        }}
      />
      <button
        type="button"
        data-testid="person-approver-save"
        disabled={save.isPending}
        onClick={() => {
          save.reset();
          save.mutate(approver);
        }}
      >
        Set approver
      </button>
      {save.isError && save.submittedAt >= latest && (
        <span data-testid="person-error" role="alert">
          {' '}
          {save.error instanceof HttpError && save.error.reason ? `${save.error.reason}.` : 'The approver was refused.'}
        </span>
      )}
    </td>
  );
}

export const contributions = [
  PersonColumnHeads.fill({ order: 19.65, Component: ApproverHead }),
  PersonColumns.fill({ order: 19.65, Component: PersonApprover }),
];
