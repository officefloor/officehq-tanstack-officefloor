import { useState, type FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { byUser } from '../session/session';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { claimsKey, claimsPath } from './claims';
import type { Claim } from './claims';

type Pasted = { added: number; claims: Claim[] };

// The paste box: lines of name,amount,date,note (the note may hold commas; a blank date is the sheet's today; the
// note may be absent; a note ending in " #<reference>" gives the claim's reference) each add a claim, in order, for the person of that name on the sheet (ignoring case),
// and open it as the open button would; a line matching a person's DRAFT by date and note edits that DRAFT to its amount and opens it instead.
// The server checks every line by the claim form's rules first, and any refusal (of a line, or of an open) changes nothing. Shows how many lines went through.
function PasteClaims({ sheet, user }: { sheet: string; user: string }) {
  const queryClient = useQueryClient();
  const [text, setText] = useState('');
  const paste = useMutation({
    mutationFn: (body: string) => postJson<Pasted>(byUser(`${claimsPath(sheet)}/paste`, user), { text: body }),
    onSuccess: (result) => {
      queryClient.setQueryData(claimsKey(sheet), result.claims);
      setText('');
    },
  });
  const send = useSubmitOnce(paste);

  const submit = (e: FormEvent) => {
    e.preventDefault();
    send(text);
  };

  return (
    <form onSubmit={submit}>
      <label>
        Paste claims (name,amount,date,note per line)
        <br />
        <textarea
          data-testid="paste-claims"
          rows={4}
          cols={50}
          value={text}
          onChange={(e) => {
            setText(e.target.value);
            paste.reset();
          }}
        />
      </label>
      <br />
      <button type="submit" data-testid="paste-submit" disabled={paste.isPending}>
        Add pasted claims
      </button>
      {paste.isSuccess && (
        <>
          {' '}Added <strong data-testid="paste-added">{paste.data.added}</strong>
        </>
      )}
      {paste.isError && (
        <span data-testid="paste-error" role="alert">
          {' '}Nothing was added: every line needs a name on the sheet and an amount of 0.00 or more with up to two
          decimals, dated outside any closed period and not after the person's retirement, and each claim must be
          one you may open; a line matching a claim that is no longer a DRAFT is refused.
        </span>
      )}
    </form>
  );
}

export const contribution = SheetPanels.fill({ order: 23, Component: PasteClaims });
