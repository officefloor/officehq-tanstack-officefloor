import { createFileRoute } from '@tanstack/react-router';
import { ClientsPage } from '../features/clients/ClientsPage';

// The /clients page — one new file under routes/; the route tree is generated from this directory.
export const Route = createFileRoute('/clients/')({
  component: ClientsPage,
});
