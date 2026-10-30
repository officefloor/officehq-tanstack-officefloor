import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { formatCents } from '../rates/due';
import type { Person } from '../people/usePeople';
import { useSheetFigures } from '../people/useSheetFigures';
import { poolsKey } from '../pools/usePools';
import { crewDueCents, crewsKey, useCrews, type Crew } from './useCrews';

// The sheet's crews and the form that adds one: a name (a blank name, or one already a crew, is refused)
// and a lead chosen from the people. Each crew shows its lead and its due-total — the lead's and the
// members' due-totals added up, n/a skipped — and adds a member from its row: the lead, or a person
// already in the crew, is refused. A name already a pool is refused too: every crew is also a pool of its name
// (see PoolPanel), so changing a crew changes that pool.
export function CrewPanel() {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const crews = useCrews(sheet);
  const { people, byId, totalOf } = useSheetFigures(sheet);
  const [name, setName] = useState('');
  const [leadId, setLeadId] = useState('');
  const queryClient = useQueryClient();
  const submitOnce = useSubmitOnce();
  const add = useMutation({
    mutationFn: (body: { name: string; leadId: string }) =>
      postJson<Crew[]>(`/api/sheets/${encodeURIComponent(sheet)}/crews`, { ...body, user }),
    onSuccess: (saved) => {
      setName('');
      setLeadId('');
      queryClient.setQueryData(crewsKey(sheet), saved);
      void queryClient.invalidateQueries({ queryKey: poolsKey(sheet) });
    },
  });
  const dueTotalOf = (crew: Crew) => formatCents(crewDueCents(crew, byId, totalOf));
  return (
    <>
      <ul data-testid="crew-list">
        {crews.map((c) => (
          <CrewRow key={c.name} crew={c} dueTotal={dueTotalOf(c)} people={people} sheet={sheet} user={user} />
        ))}
      </ul>
      <form
        data-testid="crew-form"
        onSubmit={(e) => {
          e.preventDefault();
          submitOnce(() => {
            add.reset();
            return add.mutateAsync({ name: name.trim(), leadId });
          });
        }}
      >
        <input
          data-testid="crew-form-name"
          value={name}
          placeholder="Crew name"
          onChange={(e) => {
            setName(e.target.value);
            add.reset();
          }}
        />{' '}
        <select
          data-testid="crew-form-lead"
          value={leadId}
          onChange={(e) => {
            setLeadId(e.target.value);
            add.reset();
          }}
        >
          <option value="">Choose a lead</option>
          {people.map((p) => (
            <option key={p.id} value={p.id}>{p.name}</option>
          ))}
        </select>{' '}
        <button type="submit" data-testid="crew-form-submit">Add crew</button>
        {add.isError && <span data-testid="crew-form-error" role="alert"> Could not add the crew: its name must not be blank or already a crew or a pool, and it needs a lead.</span>}
      </form>
    </>
  );
}

function CrewRow({ crew, dueTotal, people, sheet, user }: { crew: Crew; dueTotal: string; people: Person[]; sheet: string; user: string }) {
  const [personId, setPersonId] = useState('');
  const queryClient = useQueryClient();
  const submitOnce = useSubmitOnce();
  const add = useMutation({
    mutationFn: (body: { personId: string }) =>
      postJson<Crew[]>(`/api/sheets/${encodeURIComponent(sheet)}/crew-members`, { ...body, crew: crew.name, user }),
    onSuccess: (saved) => {
      setPersonId('');
      queryClient.setQueryData(crewsKey(sheet), saved);
      void queryClient.invalidateQueries({ queryKey: poolsKey(sheet) });
    },
  });
  return (
    <li data-testid={`crew-row-${crew.name}`}>
      <span data-testid="crew-name">{crew.name}</span>{' '}
      lead <span data-testid="crew-lead">{crew.leadName}</span>{' '}
      due-total <span data-testid="crew-due-total">{dueTotal}</span>
      <ul>
        {crew.members.map((m) => (
          <li key={m.personId} data-testid="crew-member-line">
            <span data-testid="crew-member-name">{m.name}</span>
          </li>
        ))}
      </ul>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          submitOnce(() => {
            add.reset();
            return add.mutateAsync({ personId });
          });
        }}
      >
        <select
          data-testid="crew-member-select"
          value={personId}
          onChange={(e) => {
            setPersonId(e.target.value);
            add.reset();
          }}
        >
          <option value="">Choose a person</option>
          {people.map((p) => (
            <option key={p.id} value={p.id}>{p.name}</option>
          ))}
        </select>{' '}
        <button type="submit" data-testid="crew-member-add">Add member</button>
        {add.isError && <span data-testid="crew-error" role="alert"> Could not add the member: they lead the crew or are already in it.</span>}
      </form>
    </li>
  );
}
