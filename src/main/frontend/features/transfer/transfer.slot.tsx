import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PersonCells, PersonHeads } from '../../slots/defs/personRow';
import { type Person } from '../people/people.slot';
import { personActionKey, useLastPersonAction } from '../people/personAction';
import { useUser } from '../session/useSession';

function TransferHeads() {
  return <th>Transfer</th>;
}

// The other sheet's id and the button that transfers this person there: a steward's only, never a retired person nor
// to this sheet itself. The person leaves this sheet entirely, so the whole sheet is re-read. A refusal shows on the
// person until the entry is changed or the next action on their row.
function TransferCells({ sheetId, personId }: { sheetId: string; user: string; personId: number }) {
  const [to, setTo] = useState('');
  const [user] = useUser();
  const queryClient = useQueryClient();
  const transfer = useMutation({
    mutationKey: personActionKey(sheetId, personId),
    mutationFn: async (toSheet: string) => {
      if (toSheet.trim() === '') throw new Error('Enter the sheet id to transfer to');
      return postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/transfer`, {
        to: toSheet.trim(),
        by: user ?? '',
      });
    },
    onSuccess: async () => {
      setTo('');
      await queryClient.invalidateQueries({ queryKey: ['sheet', sheetId] });
    },
  });
  const last = useLastPersonAction(sheetId, personId, transfer.submittedAt);

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
      />{' '}
      <button type="button" data-testid="person-transfer" disabled={transfer.isPending} onClick={() => transfer.mutate(to)}>
        Transfer
      </button>
      {transfer.isError && last && (
        <span data-testid="person-error" role="alert">
          {transfer.error.message}
        </span>
      )}
    </td>
  );
}

export const contributions = [
  PersonHeads.fill({ order: 58, Component: TransferHeads }),
  PersonCells.fill({ order: 58, Component: TransferCells }),
];
