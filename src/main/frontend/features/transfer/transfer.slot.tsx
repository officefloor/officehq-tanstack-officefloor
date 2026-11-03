import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PersonCells, PersonColumnHead } from '../../slots/defs/personRow';
import { usePeople, type Person } from '../people/usePeople';
import { usePersonRowAction } from '../people/usePersonRowAction';

/** A steward transfers a person to another sheet by id: an entry for the sheet id and a transfer button. The
 *  person, with their grades, terms, contact, aliases and claims, leaves this sheet for the other, so every
 *  figure on the sheet is re-read. Anyone else, a retired person, this sheet's own id, or a name, alias or
 *  contact already on the other sheet is refused with the row's error. */
function PersonTransfer({ sheetId, personId }: { sheetId: string; personId: number }) {
  const queryClient = useQueryClient();
  const [to, setTo] = useState('');

  const transfer = useMutation({
    mutationFn: (toSheetId: string) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/transfer`, { toSheetId }),
    onSuccess: async () => {
      setTo('');
      await queryClient.invalidateQueries({ queryKey: ['sheet', sheetId] });
    },
  });
  const announce = usePersonRowAction(sheetId, personId, transfer.reset);

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
      <button
        type="button"
        data-testid="person-transfer"
        disabled={transfer.isPending}
        onClick={() => {
          announce();
          transfer.reset();
          transfer.mutate(to);
        }}
      >
        Transfer
      </button>
      {transfer.isError && <span data-testid="person-error">{transfer.error.message}</span>}
    </td>
  );
}

/** A steward undoes the transfer that brought a person here, on the sheet-today they arrived: they go back to the
 *  sheet they came from by the transfer rules, so every figure on the sheet is re-read. Any later day, anyone else,
 *  or a person who did not arrive by transfer is refused with the row's error. */
function PersonTransferUndo({ sheetId, personId }: { sheetId: string; personId: number }) {
  const queryClient = useQueryClient();
  const undo = useMutation({
    mutationFn: () => postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/transfer-undo`, {}),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['sheet', sheetId] });
    },
  });
  const announce = usePersonRowAction(sheetId, personId, undo.reset);

  return (
    <td>
      <button
        type="button"
        data-testid="person-transfer-undo"
        disabled={undo.isPending}
        onClick={() => {
          announce();
          undo.reset();
          undo.mutate();
        }}
      >
        Undo transfer
      </button>
      {undo.isError && <span data-testid="person-error">{undo.error.message}</span>}
    </td>
  );
}

/** The sheet a person was transferred from (blank for anyone added on this sheet). */
function PersonOrigin({ sheetId, personId }: { sheetId: string; personId: number }) {
  const people = usePeople(sheetId);
  const p = people.data?.find((x) => x.id === personId);
  return <td data-testid="person-origin">{p?.origin ?? ''}</td>;
}

export const contributions = [
  PersonColumnHead.fill({ order: 55, Component: () => <th>From sheet</th> }),
  PersonCells.fill({ order: 55, Component: PersonOrigin }),
  PersonColumnHead.fill({ order: 56, Component: () => <th>Transfer</th> }),
  PersonCells.fill({ order: 56, Component: PersonTransfer }),
  PersonColumnHead.fill({ order: 56.5, Component: () => <th>Undo transfer</th> }),
  PersonCells.fill({ order: 56.5, Component: PersonTransferUndo }),
];
