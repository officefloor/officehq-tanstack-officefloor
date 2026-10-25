import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { useRowAction } from '../../api/rowAction';
import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useCurrentSheet, useCurrentUser, useSheetToday } from '../session/session';
import { peoplePath, peopleQueryKey, type Person } from './people';

// A steward's flag on a person: the reason it shows while the sheet's today is on or before its until-date
// (nothing after it), and the reason, until-date (blank: the sheet's today) and button that flag them. While
// it is in force only a steward may open their claims (alone, in bulk or by a paste). Anyone else, or a blank
// reason, is refused.
function FlagHeader() {
  return <th>Flag</th>;
}

function PersonFlag({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const today = useSheetToday();
  const [reason, setReason] = useState('');
  const [until, setUntil] = useState('');
  const queryClient = useQueryClient();
  const flag = useMutation({
    mutationFn: (body: { reason: string; until: string }) =>
      putJson<Person>(`${peoplePath(sheet ?? '')}/${person.id}/flag`, body, user),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: peopleQueryKey(sheet ?? '') });
      setReason('');
      setUntil('');
    },
  });
  const submit = useSubmitOnce(flag);
  const rowAction = useRowAction(`person-${person.id}`);
  return (
    <td>
      <span data-testid="person-flag-shown">{person.flag ?? ''}</span>{' '}
      <input
        data-testid="person-flag-reason"
        placeholder="reason"
        value={reason}
        onChange={(e) => { setReason(e.target.value); flag.reset(); }}
      />
      <input
        data-testid="person-flag-until"
        placeholder={today ?? 'until'}
        value={until}
        onChange={(e) => { setUntil(e.target.value); flag.reset(); }}
      />
      <button type="button" data-testid="person-flag" disabled={flag.isPending} onClick={() => { rowAction.take(); submit({ reason, until }); }}>
        Flag
      </button>
      {flag.isError && rowAction.isLatest && <span data-testid="person-error">Only a steward can flag a person, with a reason and a calendar until-date.</span>}
    </td>
  );
}

export const contributions = [
  PeopleColumns.fill({ order: 45.5, Component: FlagHeader }),
  PersonCells.fill({ order: 45.5, Component: PersonFlag }),
];
