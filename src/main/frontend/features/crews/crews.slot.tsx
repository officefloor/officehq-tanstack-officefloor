import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { postJson, RefusedError } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { useUser } from '../session/useUser';
import { peopleQuery } from '../people/peopleQuery';
import type { Person } from '../people/peopleQuery';
import { sumDue } from '../rates/ratesQuery';
import { usePeopleDueTotals } from '../rates/due.slot';
import { poolsKey } from '../pools/poolsQuery';
import { crewsKey, crewsQuery } from './crewsQuery';
import type { Crew } from './crewsQuery';

/** A crew's due-total: the lead's and the members' due-totals added up, n/a skipped. */
export function crewDue(crew: Crew, dues: Map<number, string>): string {
  const ids = [crew.leadId, ...crew.memberIds].filter((id) => dues.has(id));
  return sumDue(ids.map((id) => dues.get(id) ?? 'n/a'));
}

/** One crew's row: its lead, its due-total (the lead's and the members' due-totals added up, n/a skipped), its members,
 *  and the form a member is added through. The lead, or a person already in the crew, is refused with an error on the crew. */
function CrewRow({ sheetId, crew, people, dues }: { sheetId: string; crew: Crew; people: Person[]; dues: Map<number, string> }) {
  const queryClient = useQueryClient();
  const [personId, setPersonId] = useState('');
  const [error, setError] = useState('');

  const [user] = useUser();
  const once = useSubmitOnce();
  const add = useMutation({
    mutationFn: (body: { crew: string; personId: number | null }) =>
      postJson(`/api/sheets/${encodeURIComponent(sheetId)}/crew-members`, body, user),
    onSuccess: async () => {
      // A crew is also a pool, so its change re-reads the pools too.
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: crewsKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) }),
      ]);
      setPersonId('');
    },
    onError: (e: Error) => setError(e instanceof RefusedError ? e.message : 'The member could not be added.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    once(() => add.mutateAsync({ crew: crew.name, personId: personId === '' ? null : Number(personId) }));
  };

  const nameOf = (id: number) => people.find((p) => p.id === id)?.name;

  return (
    <li data-testid={`crew-row-${crew.name}`}>
      <strong data-testid="crew-name">{crew.name}</strong> — lead <span data-testid="crew-lead">{nameOf(crew.leadId) ?? ''}</span>,
      due <span data-testid="crew-due-total">{crewDue(crew, dues)}</span>
      <ul>
        {crew.memberIds
          .filter((id) => nameOf(id) !== undefined)
          .map((id) => (
            <li key={id} data-testid="crew-member-line">
              <span data-testid="crew-member-name">{nameOf(id)}</span>
            </li>
          ))}
      </ul>
      <form onSubmit={submit}>
        <select
          data-testid="crew-member-select"
          value={personId}
          onChange={(e) => {
            setPersonId(e.target.value);
            setError('');
          }}
        >
          <option value="">Choose a person</option>
          {people.map((p) => (
            <option key={p.id} value={String(p.id)}>
              {p.name}
            </option>
          ))}
        </select>{' '}
        <button type="submit" data-testid="crew-member-add" disabled={add.isPending}>
          Add member
        </button>
        {error && <p data-testid="crew-error">{error}</p>}
      </form>
    </li>
  );
}

/** The sheet's crews, and the form a crew is added through: a name and a lead. A blank name, or one already a crew, is refused. */
function Crews({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const crews = useQuery(crewsQuery(sheetId));
  const people = useQuery(peopleQuery(sheetId)).data ?? [];
  const dues = usePeopleDueTotals(sheetId);
  const [name, setName] = useState('');
  const [leadId, setLeadId] = useState('');
  const [error, setError] = useState('');

  const [user] = useUser();
  const once = useSubmitOnce();
  const add = useMutation({
    mutationFn: (body: { name: string; leadId: number | null }) =>
      postJson(`/api/sheets/${encodeURIComponent(sheetId)}/crews`, body, user),
    onSuccess: async () => {
      // A crew is also a pool, so its change re-reads the pools too.
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: crewsKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) }),
      ]);
      setName('');
      setLeadId('');
    },
    onError: (e: Error) => setError(e instanceof RefusedError ? e.message : 'The crew could not be added.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    once(() => add.mutateAsync({ name: name.trim(), leadId: leadId === '' ? null : Number(leadId) }));
  };

  return (
    <section data-testid="crews">
      <h3>Crews</h3>
      <form onSubmit={submit}>
        <input
          data-testid="crew-form-name"
          placeholder="Crew name"
          value={name}
          onChange={(e) => {
            setName(e.target.value);
            setError('');
          }}
        />{' '}
        <select
          data-testid="crew-form-lead"
          value={leadId}
          onChange={(e) => {
            setLeadId(e.target.value);
            setError('');
          }}
        >
          <option value="">Choose the lead</option>
          {people.map((p) => (
            <option key={p.id} value={String(p.id)}>
              {p.name}
            </option>
          ))}
        </select>{' '}
        <button type="submit" data-testid="crew-form-submit" disabled={add.isPending}>
          Add crew
        </button>
        {error && <p data-testid="crew-form-error">{error}</p>}
      </form>
      {crews.isError && <p data-testid="crews-load-error">Could not load the crews.</p>}
      <ul>
        {(crews.data ?? []).map((c) => (
          <CrewRow key={c.name} sheetId={sheetId} crew={c} people={people} dues={dues} />
        ))}
      </ul>
    </section>
  );
}

// Keyed by sheet, so a typed value or an error never carries over to another sheet.
export const contribution = SheetPanel.fill({
  order: 6.9,
  Component: ({ sheetId }) => <Crews key={`crews-${sheetId}`} sheetId={sheetId} />,
});
