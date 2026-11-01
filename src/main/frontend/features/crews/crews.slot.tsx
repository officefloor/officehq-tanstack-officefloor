import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { useSheetTotals } from '../claims/useSheetTotals';
import { usePeople, type Person } from '../people/usePeople';
import { totalDue } from '../rates/useRates';
import { crewDues, crewsKey, useCrews, type Crew } from './useCrews';

/**
 * The sheet's crews and a form to add one: a name (trimmed; a blank name, or one already a crew, is
 * refused) and a lead chosen from the people. Each crew's row adds a member chosen from the people;
 * the crew's lead, or a person already in the crew, is refused. A crew shows its lead and its
 * due-total: the lead's and the members' due-totals added up, an n/a one skipped.
 */
function CrewsPanel({ user, sheet }: { user: string; sheet: string }) {
  const queryClient = useQueryClient();
  const { data: crews = [] } = useCrews(sheet);
  const { data: people = [] } = usePeople(sheet);
  const { perPerson } = useSheetTotals(sheet);
  const [name, setName] = useState('');
  const [leadId, setLeadId] = useState('');
  const [invalid, setInvalid] = useState(false);

  const submitOnce = useSubmitOnce();
  const add = useMutation({
    mutationFn: (c: { name: string; leadId: number }) =>
      postJson<Crew[]>(`/api/sheets/${encodeURIComponent(sheet)}/crews`, { ...c, user }),
    onSuccess: async () => {
      setName('');
      setLeadId('');
      await queryClient.invalidateQueries({ queryKey: crewsKey(sheet) });
    },
  });

  // An error goes as soon as its form is changed or submitted again.
  const clearError = () => {
    add.reset();
    setInvalid(false);
  };

  return (
    <section data-testid="crews-panel">
      <h2>Crews</h2>
      <form
        data-testid="crew-form"
        onSubmit={(e) => {
          e.preventDefault();
          const n = name.trim();
          if (n === '' || leadId === '' || crews.some((c) => c.name === n)) {
            // Refused here, before anything is sent: the store and the audit file are untouched.
            add.reset();
            setInvalid(true);
            return;
          }
          setInvalid(false);
          submitOnce(() => add.mutateAsync({ name: n, leadId: Number(leadId) }));
        }}
      >
        <input
          data-testid="crew-form-name"
          aria-label="Crew name"
          placeholder="name"
          value={name}
          onChange={(e) => {
            setName(e.target.value);
            clearError();
          }}
        />{' '}
        <select
          data-testid="crew-form-lead"
          aria-label="Lead"
          value={leadId}
          onChange={(e) => {
            setLeadId(e.target.value);
            clearError();
          }}
        >
          <option value="">lead…</option>
          {people.map((p) => (
            <option key={p.id} value={String(p.id)}>
              {p.name}
            </option>
          ))}
        </select>{' '}
        <button type="submit" data-testid="crew-form-submit" disabled={add.isPending}>
          Add crew
        </button>
        {(invalid || add.isError) && (
          <span data-testid="crew-form-error" role="alert">
            {' '}
            {invalid ? 'Enter a name not already a crew, and choose a lead.' : 'Could not add this crew.'}
          </span>
        )}
      </form>
      <table data-testid="crew-list">
        <tbody>
          {crews.map((c) => (
            <CrewRow
              key={c.name}
              user={user}
              sheet={sheet}
              crew={c}
              people={people}
              dueTotal={totalDue(crewDues(c, perPerson))}
            />
          ))}
        </tbody>
      </table>
    </section>
  );
}

/** A crew's row: its lead, its due-total, its members, and a form to add one. */
function CrewRow({
  user,
  sheet,
  crew,
  people,
  dueTotal,
}: {
  user: string;
  sheet: string;
  crew: Crew;
  people: Person[];
  dueTotal: string;
}) {
  const queryClient = useQueryClient();
  const [personId, setPersonId] = useState('');
  const [invalid, setInvalid] = useState(false);

  const submitOnce = useSubmitOnce();
  const add = useMutation({
    mutationFn: (id: number) =>
      postJson<Crew[]>(`/api/sheets/${encodeURIComponent(sheet)}/crews/members`, { crew: crew.name, personId: id, user }),
    onSuccess: async () => {
      setPersonId('');
      await queryClient.invalidateQueries({ queryKey: crewsKey(sheet) });
    },
  });

  const clearError = () => {
    add.reset();
    setInvalid(false);
  };

  return (
    <tr data-testid={`crew-row-${crew.name}`}>
      <td data-testid="crew-name">{crew.name}</td>
      <td>
        lead <span data-testid="crew-lead">{crew.lead ?? ''}</span>
      </td>
      <td>
        due-total <span data-testid="crew-due-total">{dueTotal}</span>
      </td>
      <td>
        <ul>
          {crew.members.map((m) => (
            <li key={m.personId} data-testid="crew-member-line">
              {m.name}
            </li>
          ))}
        </ul>
      </td>
      <td>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            const id = Number(personId);
            if (personId === '' || id === crew.leadId || crew.members.some((m) => m.personId === id)) {
              // Refused here, before anything is sent: the store and the audit file are untouched.
              add.reset();
              setInvalid(true);
              return;
            }
            setInvalid(false);
            submitOnce(() => add.mutateAsync(id));
          }}
        >
          <select
            data-testid="crew-member-select"
            aria-label="Member"
            value={personId}
            onChange={(e) => {
              setPersonId(e.target.value);
              clearError();
            }}
          >
            <option value="">choose…</option>
            {people.map((p) => (
              <option key={p.id} value={String(p.id)}>
                {p.name}
              </option>
            ))}
          </select>{' '}
          <button type="submit" data-testid="crew-member-add" disabled={add.isPending}>
            Add member
          </button>
          {(invalid || add.isError) && (
            <span data-testid="crew-error" role="alert">
              {' '}
              {invalid ? 'Choose a person who is not the lead and not already in the crew.' : 'Could not add this member.'}
            </span>
          )}
        </form>
      </td>
    </tr>
  );
}

export const contribution = SheetPanels.fill({ order: 10.2, Component: CrewsPanel });
