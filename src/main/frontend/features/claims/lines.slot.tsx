import { useState } from 'react';
import type { ChangeEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson, RefusedError } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { ClaimCells } from '../../slots/defs/claimColumns';
import { useUser } from '../session/useUser';
import { claimsKey } from './claimsQuery';
import type { Claim } from './claimsQuery';
import { useClaimError } from './useClaimError';

type LineFields = { label: string; qty: string; unit: string };

const blank: LineFields = { label: '', qty: '', unit: '' };

/**
 * The claim's lines, in the order added, each as "<label> <qty> x <unit>", and the entries and button that add one. A
 * line edits the claim: its amount becomes the lines added up. The server refuses an empty label, a quantity under 1, a
 * unit amount with more than two decimals, and a claim that is not a draft, is in a closed period or another user has
 * locked; the refusal shows on the claim until its next action.
 */
function ClaimLines({ sheetId, claim }: { sheetId: string; claim: Claim }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [, setError] = useClaimError(claim.id);
  const [fields, setFields] = useState<LineFields>(blank);
  const once = useSubmitOnce();
  const add = useMutation({
    mutationFn: (body: LineFields) =>
      postJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/lines`, body, user),
    onSuccess: () => {
      setFields(blank);
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onError: (e) => setError(e instanceof RefusedError ? e.message : 'The line could not be added.'),
  });

  const field = (key: keyof LineFields) => ({
    'data-testid': `claim-line-${key}`,
    placeholder: key,
    value: fields[key],
    onChange: (e: ChangeEvent<HTMLInputElement>) => {
      setFields((f) => ({ ...f, [key]: e.target.value }));
      setError('');
    },
  });

  return (
    <td data-testid="claim-lines">
      {(claim.lines ?? []).map((line, i) => (
        <div key={i} data-testid="claim-lines-line">
          {line}
        </div>
      ))}
      <input {...field('label')} />
      <input {...field('qty')} size={4} />
      <input {...field('unit')} size={8} />
      <button
        type="button"
        data-testid="claim-line-add"
        disabled={add.isPending}
        onClick={() => {
          setError('');
          once(() => add.mutateAsync(fields));
        }}
      >
        Add line
      </button>
    </td>
  );
}

export const contribution = ClaimCells.fill({ order: 92, Component: ClaimLines });
