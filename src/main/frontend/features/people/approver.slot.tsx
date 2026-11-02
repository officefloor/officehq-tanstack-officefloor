import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { useIsLatest } from '../../query/useIsLatest';
import { byUser } from '../session/session';
import { PersonColumnHeaders, PersonColumns } from '../../slots/defs/personRow';
import { peopleKey, peoplePath, personActionKey } from './people';
import type { Person } from './people';

function ApproverHeader() {
  return (
    <>
      <th>Approver</th>
      <th />
    </>
  );
}

// The person's approver (a user name; blank when none), and the entry and button with which it is set (a blank entry
// clears it). The approver may approve or reject the person's PENDING claims as a steward may.
function PersonApprover({ sheet, user, person }: { sheet: string; user: string; person: Person }) {
  const queryClient = useQueryClient();
  const [approver, setApprover] = useState('');
  const save = useMutation({
    mutationKey: personActionKey(sheet, person.id),
    mutationFn: (body: { user: string }) =>
      postJson<Person[]>(byUser(`${peoplePath(sheet)}/${person.id}/approver`, user), body),
    onSuccess: (updated) => {
      queryClient.setQueryData(peopleKey(sheet), updated);
      setApprover('');
    },
  });
  const act = useSubmitOnce(save);
  const latest = useIsLatest(personActionKey(sheet, person.id), save.submittedAt);

  return (
    <>
      <td data-testid="person-approver">{person.approver ?? ''}</td>
      <td>
        <input
          data-testid="person-approver-input"
          type="text"
          placeholder="approver"
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
          onClick={() => act({ user: approver.trim() })}
        >
          Set approver
        </button>
        {save.isError && latest && (
          <span data-testid="person-error" role="alert">
            {' '}That approver was refused.
          </span>
        )}
      </td>
    </>
  );
}

export const contributions = [
  PersonColumnHeaders.fill({ order: 43, Component: ApproverHeader }),
  PersonColumns.fill({ order: 43, Component: PersonApprover }),
];
