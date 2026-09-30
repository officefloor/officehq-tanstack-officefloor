import { ClientDetail } from '../../slots/defs/clientDetail';
import { useSearchParam, asFlag } from '../../url/useSearchParam';
import { CLIENT_PROJECTS_SHOW_ALL_PARAM } from './queries';

// The "show all projects" toggle on a client's detail page — one new *.slot.tsx file filling the
// ClientDetail region (CLAUDE.md rule 3). Nothing existing is edited to add it. It owns the
// `clientProjectsAll` URL key (rule 4): whether the finished and hidden projects are revealed
// outlives a click, so it lives in the URL, and the projects table reads the same key to decide
// which rows show. No callback, no shared state — just the key.
function ShowAllProjectsToggle() {
  const [showAll, setShowAll] = useSearchParam(CLIENT_PROJECTS_SHOW_ALL_PARAM, asFlag);
  return (
    <label>
      <input
        type="checkbox"
        data-testid="client-projects-show-all"
        checked={showAll}
        onChange={(event) => setShowAll(event.target.checked ? true : undefined)}
      />
      Show finished and hidden projects
    </label>
  );
}

export const contribution = ClientDetail.fill({ order: 10, Component: ShowAllProjectsToggle });
