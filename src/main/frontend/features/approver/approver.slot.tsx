import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PeopleHead, PersonRow } from '../../slots/defs/personRow';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { type Person } from '../people/people';
import { usePersonRowAction } from '../people/rowAction';
import { approversKey, useApprovers, type Approver } from './approver';

function ApproverHead() {
  return <th>Approver</th>;
}

/** The person's approver (blank: none), and the user name and button that set it. The approver may approve or reject
 *  the person's PENDING claims as a steward may. A refused change shows the row's error. */
function PersonApprover({ sheetId, user, person }: { sheetId: string; user: string; person: Person }) {
  const queryClient = useQueryClient();
  const approvers = useApprovers(sheetId);
  const [entry, setEntry] = useState('');
  const submit = useSubmitOnce();
  const rowAction = usePersonRowAction(sheetId, person.id, 'approver');
  const approver = (approvers.data ?? []).find((a) => a.personId === person.id)?.user ?? '';
  const save = useMutation({
    mutationFn: (to: string) =>
      postJson<Approver>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/approver`, { user: to, by: user }),
    onSuccess: () => {
      setEntry('');
      return queryClient.invalidateQueries({ queryKey: approversKey(sheetId) });
    },
    onSettled: submit.end,
  });

  return (
    <td>
      <span data-testid="person-approver">{approver}</span>{' '}
      <input
        data-testid="person-approver-input"
        placeholder="user"
        value={entry}
        onChange={(e) => {
          setEntry(e.target.value);
          save.reset();
        }}
      />{' '}
      <button
        type="button"
        data-testid="person-approver-save"
        disabled={save.isPending}
        onClick={() => {
          if (!submit.begin()) return;
          rowAction.take();
          save.reset();
          save.mutate(entry.trim());
        }}
      >
        Set approver
      </button>
      {save.isError && rowAction.isLatest && <span data-testid="person-error"> Could not set this person's approver.</span>}
    </td>
  );
}

export const contributions = [
  PeopleHead.fill({ order: 72, Component: ApproverHead }),
  PersonRow.fill({ order: 72, Component: PersonApprover }),
];
