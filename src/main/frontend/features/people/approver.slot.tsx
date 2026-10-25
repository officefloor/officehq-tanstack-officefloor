import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { useRowAction } from '../../api/rowAction';
import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { peoplePath, peopleQueryKey, type Person } from './people';

// A person's approver: the user name it shows (nothing with none), and the entry and button that set it (blank:
// none). That user may approve or reject the person's PENDING claims as a steward may (though not one they
// opened or last edited). A viewer, or an archived sheet, is refused.
function ApproverHeader() {
  return <th>Approver</th>;
}

function PersonApprover({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [approver, setApprover] = useState('');
  const queryClient = useQueryClient();
  const save = useMutation({
    mutationFn: (body: { user: string }) =>
      putJson<Person>(`${peoplePath(sheet ?? '')}/${person.id}/approver`, body, user),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: peopleQueryKey(sheet ?? '') });
      setApprover('');
    },
  });
  const submit = useSubmitOnce(save);
  const rowAction = useRowAction(`person-${person.id}`);
  return (
    <td>
      <span data-testid="person-approver">{person.approver ?? ''}</span>{' '}
      <input
        data-testid="person-approver-input"
        placeholder="approver"
        value={approver}
        onChange={(e) => { setApprover(e.target.value); save.reset(); }}
      />
      <button type="button" data-testid="person-approver-save" disabled={save.isPending} onClick={() => { rowAction.take(); submit({ user: approver }); }}>
        Set approver
      </button>
      {save.isError && rowAction.isLatest && <span data-testid="person-error">The approver could not be set.</span>}
    </td>
  );
}

export const contributions = [
  PeopleColumns.fill({ order: 45.6, Component: ApproverHeader }),
  PersonCells.fill({ order: 45.6, Component: PersonApprover }),
];
