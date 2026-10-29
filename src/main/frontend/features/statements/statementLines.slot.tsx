import { useQuery } from '@tanstack/react-query';
import { PersonCells, PersonHeaders } from '../../slots/defs/personColumns';
import { statementsQuery } from './statementsQuery';

/** The person's statement lines (see `Statements.lines`): one per statement still standing, in the order taken. */
function PersonStatementLines({ sheetId, personId }: { sheetId: string; personId: number }) {
  const shown = (useQuery(statementsQuery(sheetId)).data ?? []).find((s) => s.personId === personId);
  return (
    <td>
      {(shown?.lines ?? []).map((line, i) => (
        <div key={i} data-testid="person-statement-line-line">
          {line}
        </div>
      ))}
    </td>
  );
}

export const contributions = [
  PersonHeaders.fill({ order: 17.5, Component: () => <th>Statement lines</th> }),
  PersonCells.fill({ order: 17.5, Component: PersonStatementLines }),
];
