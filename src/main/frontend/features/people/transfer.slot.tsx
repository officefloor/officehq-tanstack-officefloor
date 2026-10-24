import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { useRowAction } from '../../api/rowAction';
import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { peoplePath, type Person } from './people';

// The id of the sheet to transfer this person to, and the button that transfers them (a steward only; not a
// retired person, nor to this sheet). They arrive there with their grades, terms, contact, aliases and claims — or are
// merged into the person there going by their name or an alias — and leave this sheet's list.
function TransferHeader() {
  return <th>Transfer</th>;
}

function PersonTransfer({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [to, setTo] = useState('');
  const queryClient = useQueryClient();
  const transfer = useMutation({
    mutationFn: (body: { toSheet: string }) =>
      putJson<unknown>(`${peoplePath(sheet ?? '')}/${person.id}/transfer`, body, user),
    onSuccess: async () => {
      setTo('');
      await queryClient.invalidateQueries({ queryKey: ['sheet', sheet ?? ''] });
    },
  });
  const submit = useSubmitOnce(transfer);
  const rowAction = useRowAction(`person-${person.id}`);
  return (
    <td>
      <input
        data-testid="person-transfer-to"
        placeholder="Sheet id"
        value={to}
        onChange={(e) => {
          setTo(e.target.value);
          transfer.reset();
        }}
      />
      <button
        type="button"
        data-testid="person-transfer"
        disabled={transfer.isPending}
        onClick={() => { rowAction.take(); submit({ toSheet: to }); }}
      >
        Transfer
      </button>
      {transfer.isError && rowAction.isLatest && (
        <span data-testid="person-error">The person could not be transferred.</span>
      )}
    </td>
  );
}

export const contributions = [
  PeopleColumns.fill({ order: 50, Component: TransferHeader }),
  PersonCells.fill({ order: 50, Component: PersonTransfer }),
];
