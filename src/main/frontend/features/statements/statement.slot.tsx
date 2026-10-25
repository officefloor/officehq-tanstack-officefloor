import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { figureToday, putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { useRowAction } from '../../api/rowAction';
import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useCurrentSheet, useCurrentUser, useRealToday } from '../session/session';
import { claimsQueryKey } from '../claims/claims';
import { peoplePath, peopleQueryKey, type Person } from '../people/people';
import { useColumnShown } from '../people/columns';
import { csvValue, readFresh, sheetInfoPath } from '../people/export.slot';
import { sheetQueryKey, type SheetInfo } from '../session/session';
import { claimsPath, type Claim } from '../claims/claims';
import { gradeOn } from '../people/people';
import { chargedOf, ratesPath, ratesQueryKey, type Rate } from '../rates/rates';
import { settingsPath, settingsQueryKey, type Settings } from '../roles/roles';

// A person's statements: one line per statement still standing, oldest first, as <date> <due-total>; how many were taken, the last one's due-total and date, and the button that takes
// one — recording their due-total now and binding each of their counted claims to the figure it has now; or, with a
// date in the entry beside it (on or before the sheet's today), as of the end of that date — and the
// button a steward cancels the last one with, making the claims it bound live again (anyone else, or a person with no
// statement, shows the row's error). The export button downloads the statement as CSV — number, date, amount, figure
// and status of each claim of theirs their last statement binds, oldest first, the figure as the claim's row shows it;
// its inputs are re-read from the store when pressed, as the other exports'.
function StatementHeader() {
  return <th>Statements</th>;
}

function PersonStatement({ person }: { person: Person }) {
  const countShown = useColumnShown('statement-count');
  const totalShown = useColumnShown('stated-total');
  const dateShown = useColumnShown('statement-date');
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const today = useRealToday();
  const [asOf, setAsOf] = useState('');
  const queryClient = useQueryClient();
  const take = useMutation({
    mutationFn: (date: string) =>
      putJson<Person>(`${peoplePath(sheet ?? '')}/${person.id}/statement`
        + (date.trim() ? `?asOf=${encodeURIComponent(date.trim())}` : ''), undefined, user),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleQueryKey(sheet ?? '') }),
        queryClient.invalidateQueries({ queryKey: claimsQueryKey(sheet ?? '') }),
      ]);
    },
    // A date typed for one statement, taken or refused, never carries into the next.
    onSettled: () => setAsOf(''),
  });
  const submit = useSubmitOnce(take);
  const rowAction = useRowAction(`person-${person.id}`);
  const cancel = useMutation({
    mutationFn: () => putJson<Person>(`${peoplePath(sheet ?? '')}/${person.id}/unstate`, undefined, user),
    onSuccess: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleQueryKey(sheet ?? '') }),
        queryClient.invalidateQueries({ queryKey: claimsQueryKey(sheet ?? '') }),
      ]),
  });
  const submitCancel = useSubmitOnce(cancel);
  const cancelAction = useRowAction(`person-${person.id}`);
  const exportStatement = async () => {
    if (!sheet) return;
    const [info, people, claims, rates, settings] = await Promise.all([
      readFresh<SheetInfo>(queryClient, sheetQueryKey(sheet), sheetInfoPath(sheet)),
      readFresh<Person[]>(queryClient, peopleQueryKey(sheet), peoplePath(sheet)),
      readFresh<Claim[]>(queryClient, claimsQueryKey(sheet), claimsPath(sheet)),
      readFresh<Rate[]>(queryClient, ratesQueryKey(sheet), ratesPath(sheet)),
      readFresh<Settings>(queryClient, settingsQueryKey(sheet), settingsPath(sheet)),
    ]);
    const p = people.find((x) => x.id === person.id) ?? person;
    const lines = [['number', 'date', 'amount', 'figure', 'status']];
    // The sheet's claims come newest first.
    for (const c of claims.filter((x) => x.personId === p.id && x.stated).reverse()) {
      lines.push([c.number ?? '', c.date, c.amount, chargedOf(c, rates, gradeOn(p, c.date), settings, figureToday(info.today)), c.status]);
    }
    const csv = lines.map((l) => l.map(csvValue).join(',') + '\n').join('');
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = `statement-${sheet}-${p.id}.csv`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  };
  return (
    <td>
      {countShown && <><span data-testid="person-statement-count">{person.statementCount}</span>{' '}</>}
      {totalShown && <><span data-testid="person-stated-total">{person.statedTotal ?? ''}</span>{' '}</>}
      {dateShown && <><span data-testid="person-statement-date">{person.statementDate ?? ''}</span>{' '}</>}
      <div>
        {person.statements.map((s, i) => (
          <div key={i} data-testid="person-statement-line-line">{s.date} {s.dueTotal}</div>
        ))}
      </div>
      <input
        data-testid="person-statement-asof"
        placeholder={today ?? 'as of'}
        value={asOf}
        onChange={(e) => { setAsOf(e.target.value); take.reset(); }}
      />
      <button type="button" data-testid="person-statement" disabled={take.isPending} onClick={() => { rowAction.take(); submit(asOf); }}>
        Statement
      </button>
      <button type="button" data-testid="person-unstate" disabled={cancel.isPending} onClick={() => { cancelAction.take(); submitCancel(undefined); }}>
        Cancel statement
      </button>
      <button type="button" data-testid="person-export-statement" onClick={() => { void exportStatement(); }}>
        Export statement (CSV)
      </button>
      {take.isError && rowAction.isLatest && <span data-testid="person-error">The statement could not be taken.</span>}
      {cancel.isError && cancelAction.isLatest && <span data-testid="person-error">The statement could not be cancelled.</span>}
    </td>
  );
}

export const contributions = [
  PeopleColumns.fill({ order: 30, Component: StatementHeader }),
  PersonCells.fill({ order: 30, Component: PersonStatement }),
];
