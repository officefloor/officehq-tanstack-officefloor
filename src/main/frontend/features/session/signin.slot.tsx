import { useState } from 'react';
import { AppHeader } from '../../slots/defs/appHeader';
import { useCurrentUser } from '../../session/session';

function SignIn() {
  const [user, setUser] = useCurrentUser();
  const [name, setName] = useState('');

  if (user) {
    return (
      <div data-testid="session">
        Signed in as <strong data-testid="current-user">{user}</strong>
      </div>
    );
  }
  return (
    <form
      data-testid="signin-form"
      onSubmit={(e) => {
        e.preventDefault();
        if (name.trim()) setUser(name.trim());
      }}
    >
      <input data-testid="signin-name" placeholder="Your name" value={name} onChange={(e) => setName(e.target.value)} />
      <button type="submit" data-testid="signin-submit">
        Sign in
      </button>
    </form>
  );
}

export const contribution = AppHeader.fill({ order: 0, Component: SignIn });
