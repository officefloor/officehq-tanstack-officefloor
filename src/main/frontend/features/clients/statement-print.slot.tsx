import { ClientDetail } from '../../slots/defs/clientDetail';
import { useSearchParam, asFlag } from '../../url/useSearchParam';
import { CLIENT_STATEMENT_PARAM } from './queries';
import { StatementPrintView } from './StatementPrintView';

// The clean, printable statement summary on a client's detail page — one new *.slot.tsx file filling
// the ClientDetail region (CLAUDE.md rule 3); nothing existing is edited to add it. It reads the same
// `clientStatement` URL key the "Statement" control already owns (rule 4), so revealing the statement
// also reveals its printable summary, with the two files sharing only that key — no import between
// them. The grand total the client owes comes straight from the statement endpoint (totalOwed).
function ClientStatementPrint({ clientId }: { clientId: number }) {
  const [open] = useSearchParam(CLIENT_STATEMENT_PARAM, asFlag);
  return open ? <StatementPrintView clientId={clientId} /> : null;
}

export const contribution = ClientDetail.fill({ order: 25, Component: ClientStatementPrint });
