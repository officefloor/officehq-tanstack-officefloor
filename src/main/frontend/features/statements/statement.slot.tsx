import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { PersonHead, PersonRow } from '../../slots/defs/personRow';
import { peopleKey } from '../people/people';
import type { Person } from '../people/people';
import { claimsKey, claimsQuery } from '../claims/claims';
import { downloadCsv } from '../export/csv';
import { useUser } from '../session/session';
import { useRowError } from '../people/rowError';
import { Col } from '../columns/columns';

function StatementHead() {
  // Downloads their statement as a CSV file: a header line, then one line per claim of theirs their
  // last statement bound, oldest first, with the bound figure as the claim shows it (n/a when it had
  // none) and its status now. The claims are read afresh when pressed, so a change just made is in it.
  const exportStatement = async () => {
    const all = await queryClient.fetchQuery({ ...claimsQuery(sheetId), staleTime: 0 });
    // The sheet's claims come newest first.
    const own = all.filter((c) => c.personId === person.id && c.stated).reverse();
    const lines = [['number', 'date', 'amount', 'figure', 'status']];
    for (const c of own) lines.push([c.number, c.date, c.amount, c.statedDue, c.status]);
    downloadCsv(`statement-${sheetId}-${person.id}.csv`, lines);
  };

  return (
    <>
      <Col name="statement-count"><th>Statements</th></Col>
      <Col name="stated-total"><th>Stated</th></Col>
      <Col name="statement-date"><th>Stated on</th></Col>
      <th>Statement lines</th>
      <th />
    </>
  );
}

// A person's statements: how many have been taken, the last one's due-total and date, and a button
// that takes the next. Taking one binds each of their claims that counts to its figure now; with a
// date beside it (on or before the sheet's today; blank: now) the statement is taken as the sheet
// stood at the end of that date, and that date is shown as its date. A steward
// can cancel the last one: the claims it bound become live again, and the one before it is shown.
function PersonStatement({ sheetId, person }: { sheetId: string; person: Person }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [asOf, setAsOf] = useState('');
  const [error, setError] = useRowError(person.id, 'statement');
  const once = useOnce();
  const take = useMutation({
    mutationFn: (date: string) =>
      postJson<Person>(
        `/api/sheets/${encodeURIComponent(sheetId)}/people/${encodeURIComponent(person.id)}/statement`,
        { asOf: date },
        user,
      ),
    // The date is used for this one statement only, whether it is taken or refused.
    onSettled: () => setAsOf(''),
    onSuccess: () => {
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
      ]);
    },
    onError: () => setError('That statement could not be taken.'),
  });
  const cancel = useMutation({
    mutationFn: () =>
      postJson<Person>(
        `/api/sheets/${encodeURIComponent(sheetId)}/people/${encodeURIComponent(person.id)}/unstate`,
        {},
        user,
      ),
    onSuccess: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
      ]),
    onError: () => setError('That statement could not be cancelled.'),
  });

  const state = () => {
    setError('');
    once((done) => take.mutate(asOf.trim(), { onSettled: done }));
  };

  const unstate = () => {
    setError('');
    once((done) => cancel.mutate(undefined, { onSettled: done }));
  };

  // Downloads their statement as a CSV file: a header line, then one line per claim of theirs their
  // last statement bound, oldest first, with the bound figure as the claim shows it (n/a when it had
  // none) and its status now. The claims are read afresh when pressed, so a change just made is in it.
  const exportStatement = async () => {
    const all = await queryClient.fetchQuery({ ...claimsQuery(sheetId), staleTime: 0 });
    // The sheet's claims come newest first.
    const own = all.filter((c) => c.personId === person.id && c.stated).reverse();
    const lines = [['number', 'date', 'amount', 'figure', 'status']];
    for (const c of own) lines.push([c.number, c.date, c.amount, c.statedDue, c.status]);
    downloadCsv(`statement-${sheetId}-${person.id}.csv`, lines);
  };

  return (
    <>
      <Col name="statement-count"><td data-testid="person-statement-count">{person.statementCount}</td></Col>
      <Col name="stated-total"><td data-testid="person-stated-total">{person.statedTotal}</td></Col>
      <Col name="statement-date"><td data-testid="person-statement-date">{person.statementDate}</td></Col>
      <td>
        {person.statementLines.map((line, i) => (
          <div key={i} data-testid="person-statement-line-line">
            {line}
          </div>
        ))}
      </td>
      <td>
        <input
          type="date"
          data-testid="person-statement-asof"
          value={asOf}
          onChange={(e) => {
            setAsOf(e.target.value);
            setError('');
          }}
        />{' '}
        <button type="button" data-testid="person-statement" onClick={state} disabled={take.isPending}>
          Statement
        </button>
        <button type="button" data-testid="person-unstate" onClick={unstate} disabled={cancel.isPending}>
          Cancel statement
        </button>
        <button type="button" data-testid="person-export-statement" onClick={() => void exportStatement()}>
          Export statement (CSV)
        </button>
        {error && (
          <p role="alert" data-testid="person-error">
            {error}
          </p>
        )}
      </td>
    </>
  );
}

export const contributions = [
  PersonHead.fill({ order: 30, Component: StatementHead }),
  PersonRow.fill({ order: 30, Component: PersonStatement }),
];
