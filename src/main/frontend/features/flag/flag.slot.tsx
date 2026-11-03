import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PersonCells, PersonColumnHead } from '../../slots/defs/personRow';
import { peopleKey, usePeople, type Person } from '../people/usePeople';
import { usePersonRowAction } from '../people/usePersonRowAction';

/** A person's flag: its reason while in force (the sheet's today on or before its until-date; nothing after), and
 *  a reason, an until-date and a button by which a steward flags them. While the flag is in force only a steward
 *  may open a claim of theirs. */
function PersonFlag({ sheetId, personId }: { sheetId: string; personId: number }) {
  const people = usePeople(sheetId);
  const queryClient = useQueryClient();
  const p = people.data?.find((x) => x.id === personId);
  const [reason, setReason] = useState('');
  const [until, setUntil] = useState('');

  const flag = useMutation({
    mutationFn: (body: { reason: string; until: string }) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/flag`, body),
    onSuccess: async () => {
      setReason('');
      setUntil('');
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
    },
  });
  const announce = usePersonRowAction(sheetId, personId, flag.reset);

  return (
    <>
      <td data-testid="person-flag-shown">{p?.flag ?? ''}</td>
      <td>
        <input
          type="text"
          placeholder="reason"
          data-testid="person-flag-reason"
          value={reason}
          onChange={(e) => {
            setReason(e.target.value);
            flag.reset();
          }}
        />{' '}
        <input
          type="text"
          placeholder="until yyyy-mm-dd"
          data-testid="person-flag-until"
          value={until}
          onChange={(e) => {
            setUntil(e.target.value);
            flag.reset();
          }}
        />{' '}
        <button
          type="button"
          data-testid="person-flag"
          disabled={flag.isPending}
          onClick={() => {
            announce();
            flag.reset();
            flag.mutate({ reason: reason.trim(), until: until.trim() });
          }}
        >
          Flag
        </button>
        {flag.isError && <span data-testid="person-error">{flag.error.message}</span>}
      </td>
    </>
  );
}

export const contributions = [
  PersonColumnHead.fill({
    order: 46,
    Component: () => (
      <>
        <th>Flag</th>
        <th />
      </>
    ),
  }),
  PersonCells.fill({ order: 46, Component: PersonFlag }),
];
