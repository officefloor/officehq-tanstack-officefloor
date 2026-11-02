import { useState, type FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { byUser } from '../session/session';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { ratesKey, ratesPath } from './rates';
import type { Rate } from './rates';

type Imported = { set: number; rates: Rate[] };

// The rate import box: lines of grade,rate,from,to (the last two may be absent or blank), in order. The server checks
// every line by the rate form's rules first, so a refusal sets nothing; otherwise each line is set as the form would
// set it, replacing the rate for the same grade and from-date. Shows how many were set.
function ImportRates({ sheet, user }: { sheet: string; user: string }) {
  const queryClient = useQueryClient();
  const [text, setText] = useState('');
  const importing = useMutation({
    mutationFn: (body: string) => postJson<Imported>(byUser(`${ratesPath(sheet)}/import`, user), { text: body }),
    onSuccess: (result) => {
      queryClient.setQueryData(ratesKey(sheet), result.rates);
      setText('');
    },
  });
  const send = useSubmitOnce(importing);

  const submit = (e: FormEvent) => {
    e.preventDefault();
    send(text);
  };

  return (
    <form onSubmit={submit}>
      <label>
        Import rates (grade,rate,from,to per line)
        <br />
        <textarea
          data-testid="import-rates"
          rows={4}
          cols={50}
          value={text}
          onChange={(e) => {
            setText(e.target.value);
            importing.reset();
          }}
        />
      </label>
      <br />
      <button type="submit" data-testid="import-rates-submit" disabled={importing.isPending}>
        Import rates
      </button>
      {importing.isSuccess && (
        <>
          {' '}Set <strong data-testid="import-rates-set">{importing.data.set}</strong>
        </>
      )}
      {importing.isError && (
        <span data-testid="import-rates-error" role="alert">
          {' '}Nothing was set: every line needs a grade of A, B or C, a rate of 0.00 or more with up to two decimals, a
          from-date and a to-date that are each a valid date or blank, and a to-date not before the from-date.
        </span>
      )}
    </form>
  );
}

export const contribution = SheetPanels.fill({ order: 5.5, Component: ImportRates });
