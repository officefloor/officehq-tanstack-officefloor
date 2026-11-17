import { useEffect, useState } from 'react';
import { useIsFetching, useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PeopleHead, PersonRow } from '../../slots/defs/personRow';
import type { Person } from '../people/people';
import { usePersonRowAction } from '../people/rowAction';
import { claimsKey, useClaims } from '../claims/claims';
import { gradeOn } from '../people/people';
import { claimDue, claimRate, useRates } from '../rates/rates';
import { useFloor, useLate } from '../settings/settings';
import { csvValue, download } from '../export/csv';
import { statementsKey, useStatements } from './statements';
import { useShownColumns } from '../columns/columns';

function StatementsHead({ user }: { user: string }) {
  const shown = useShownColumns(user);
  return (
    <>
      {shown('statement-count') && <th>Statements</th>}
      {shown('stated-total') && <th>Stated total</th>}
      {shown('statement-date') && <th>Stated on</th>}
      <th>Statement lines</th>
      <th />
      <th />
    </>
  );
}

/** A person's statement count, their last statement's due-total and date, a line per statement in force (in the order
 *  taken), and the date (blank: now) and button that
 *  take one: it records their due-total as of that date and binds each of their claims that counted then to its rate
 *  figure then (not floored); and
 *  the button that cancels their last statement (a steward only), making the claims it bound live again. */
function PersonStatements({ sheetId, user, person }: { sheetId: string; user: string; person: Person }) {
  const shown = useShownColumns(user);
  const statements = useStatements(sheetId);
  const queryClient = useQueryClient();
  const mine = statements.data?.find((s) => s.personId === person.id);
  const [asOf, setAsOf] = useState('');
  const rowAction = usePersonRowAction(sheetId, person.id, 'statement');
  const take = useMutation({
    mutationFn: (date: string) =>
      postJson(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/statements`, { by: user, asOf: date }),
    // The date is cleared once taken or refused: the next press takes one now unless a date is typed again.
    onSettled: () => setAsOf(''),
    onSuccess: () => {
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: statementsKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
      ]);
    },
  });

  const cancelAction = usePersonRowAction(sheetId, person.id, 'unstate');
  const cancel = useMutation({
    mutationFn: () =>
      postJson(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/statements/cancel`, { by: user }),
    onSuccess: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: statementsKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
      ]),
  });

  return (
    <>
      {shown('statement-count') && <td data-testid="person-statement-count">{mine?.count ?? 0}</td>}
      {shown('stated-total') && <td data-testid="person-stated-total">{mine?.dueTotal ?? ''}</td>}
      {shown('statement-date') && <td data-testid="person-statement-date">{mine?.date ?? ''}</td>}
      <td data-testid="person-statement-lines">
        {(mine?.lines ?? []).map((line, i) => (
          <div key={i} data-testid="person-statement-line-line">
            {line}
          </div>
        ))}
      </td>
      <td>
        <input
          data-testid="person-statement-asof"
          placeholder="as of yyyy-MM-dd"
          value={asOf}
          onChange={(e) => {
            setAsOf(e.target.value);
            take.reset();
          }}
        />{' '}
        <button
          type="button"
          data-testid="person-statement"
          disabled={take.isPending}
          onClick={() => {
            rowAction.take();
            take.reset();
            take.mutate(asOf.trim());
          }}
        >
          Statement
        </button>
        {take.isError && rowAction.isLatest && <span data-testid="person-error"> Could not take a statement for this person.</span>}
        <button
          type="button"
          data-testid="person-unstate"
          disabled={cancel.isPending}
          onClick={() => {
            cancelAction.take();
            cancel.reset();
            cancel.mutate();
          }}
        >
          Cancel statement
        </button>
        {cancel.isError && cancelAction.isLatest && <span data-testid="person-error"> Could not cancel this person's statement.</span>}
      </td>
    </>
  );
}

/** The button that downloads the person's statement as a CSV file: number, date, amount, figure and status of each
 *  claim of theirs bound by their last statement, oldest first, the figure as the claim row shows it. Made once the
 *  sheet's figures are no longer being re-read, so it holds what the screen shows. */
function PersonExportStatement({ sheetId, person }: { sheetId: string; person: Person }) {
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const late = useLate(sheetId);
  const floor = useFloor(sheetId);
  const fetching = useIsFetching({ queryKey: ['sheet', sheetId] });
  const [wanted, setWanted] = useState(false);

  useEffect(() => {
    if (!wanted || fetching > 0 || !claims.data) return;
    setWanted(false);
    const lines = ['number,date,amount,figure,status'];
    const bound = claims.data.filter((c) => c.personId === person.id && c.stated).reverse();
    for (const c of bound) {
      const figure = claimDue(c, claimRate(rates.data, gradeOn(person, c.date), c), floor, late);
      lines.push([c.number ?? '', c.date, c.amount, figure, c.status].map(csvValue).join(','));
    }
    download(`statement-${sheetId}-${person.id}.csv`, lines);
  }, [wanted, fetching, claims.data, rates.data, late, floor, person, sheetId]);

  return (
    <td>
      <button type="button" data-testid="person-export-statement" onClick={() => setWanted(true)}>
        Export statement (CSV)
      </button>
    </td>
  );
}

export const contributions = [
  PeopleHead.fill({ order: 30, Component: StatementsHead }),
  PersonRow.fill({ order: 30, Component: PersonStatements }),
  PersonRow.fill({ order: 30.5, Component: PersonExportStatement }),
];
