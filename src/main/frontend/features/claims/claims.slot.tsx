import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { ClaimCells } from '../../slots/defs/claimColumns';
import { PersonCells, PersonHeaders } from '../../slots/defs/personColumns';
import { claimsQuery } from './claimsQuery';
import { ClaimForm } from './ClaimForm';

/** A person's claims, newest first, with the form they are added and edited through. */
function PersonClaims({ sheetId, personId }: { sheetId: string; personId: number }) {
  const claims = (useQuery(claimsQuery(sheetId)).data ?? []).filter((c) => c.personId === personId);
  const [editingId, setEditingId] = useState<number | undefined>();
  const editing = claims.find((c) => c.id === editingId);

  return (
    <td data-testid="person-claims">
      <table>
        <tbody>
          {claims.map((c) => (
            <tr key={c.id} data-testid={`claim-row-${c.id}`}>
              <td data-testid="claim-amount">{c.amount}</td>
              <td data-testid="claim-date">{c.date}</td>
              <td data-testid="claim-note">{c.note}</td>
              <ClaimCells.Slot sheetId={sheetId} claim={c} />
              <td>
                {c.id === editing?.id ? (
                  <button type="button" data-testid="claim-cancel" onClick={() => setEditingId(undefined)}>
                    Cancel
                  </button>
                ) : (
                  <button type="button" data-testid="claim-edit" onClick={() => setEditingId(c.id)}>
                    Edit
                  </button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <ClaimForm sheetId={sheetId} personId={personId} editing={editing} onDone={() => setEditingId(undefined)} />
    </td>
  );
}

export const contributions = [
  PersonHeaders.fill({ order: 20, Component: () => <th>Claims</th> }),
  PersonCells.fill({ order: 20, Component: PersonClaims }),
];
