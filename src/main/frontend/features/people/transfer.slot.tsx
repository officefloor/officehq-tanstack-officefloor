import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { type Person, peopleKey, personActionKey, useLatestPersonAction } from './people';
import { claimsKey } from '../claims/claims';
import { poolsKey } from '../pools/pools';
import { sheetKey } from '../sheet/sheet';
import { useCurrentSheet, useCurrentUser } from '../../session/session';

function TransferHead() {
  return <th>Transfer</th>;
}

// The id of the sheet to transfer this person to, and the button that transfers them: a steward's
// action. The person, with their grades, terms, contact, aliases and claims, moves to the other sheet
// and their row leaves this one. A refused transfer shows its error until the next action is taken.
function PersonTransfer({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [to, setTo] = useState('');
  const queryClient = useQueryClient();

  const transfer = useMutation({
    mutationKey: personActionKey(sheet, person.id),
    mutationFn: (toSheet: string) =>
      postJson<unknown>(`/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/transfer`, { to: toSheet, user }),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: poolsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: [...sheetKey(sheet), 'watches'] }),
      ]);
      setTo('');
    },
  });

  const latest = useLatestPersonAction(sheet, person.id);

  return (
    <td>
      <input
        data-testid="person-transfer-to"
        placeholder="Sheet id"
        value={to}
        onChange={(e) => {
          transfer.reset();
          setTo(e.target.value);
        }}
      />
      <button
        type="button"
        data-testid="person-transfer"
        disabled={transfer.isPending}
        onClick={() => {
          transfer.reset();
          transfer.mutate(to);
        }}
      >
        Transfer
      </button>
      {transfer.isError && transfer.submittedAt >= latest && (
        <span data-testid="person-error" role="alert">
          {' '}
          {transfer.error instanceof HttpError && transfer.error.reason
            ? `${transfer.error.reason}.`
            : 'The transfer was refused.'}
        </span>
      )}
    </td>
  );
}

export const contributions = [
  PersonColumnHeads.fill({ order: 19.95, Component: TransferHead }),
  PersonColumns.fill({ order: 19.95, Component: PersonTransfer }),
];
