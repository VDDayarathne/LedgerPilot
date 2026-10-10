# LedgerPilot frontend

React 19, TypeScript, Vite 8 client for LedgerPilot Phase 1.

```powershell
npm.cmd install
npm.cmd run dev
```

The development server proxies `/api` to `http://localhost:8080`, so the default `VITE_API_URL=/api/v1` avoids cross-origin browser requests locally. Use an absolute HTTPS API URL only when the applications are intentionally deployed on separate origins; that origin must also be listed in the backend's `FRONTEND_ORIGIN` setting. The browser keeps access tokens only in memory; refresh credentials remain in backend-issued `HttpOnly` cookies. The client coordinates concurrent refresh attempts, retries a failed API call at most once, restores sessions on reload, and clears tenant state when selecting another organization.

Checks:

```powershell
npm.cmd run lint
npm.cmd run build
```
