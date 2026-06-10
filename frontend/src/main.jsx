import React, { useEffect, useMemo, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { AnalyticsPage } from './pages/AnalyticsPage.jsx';
import { DashboardPage } from './pages/DashboardPage.jsx';
import { RulesPage } from './pages/RulesPage.jsx';
import { listTenants } from './services/api.js';
import './index.css';

const nav = [
  { id: 'dashboard', label: 'Dashboard', icon: '📟' },
  { id: 'rules', label: 'Rules', icon: '⚙️' },
  { id: 'analytics', label: 'Analytics', icon: '📊' },
];

function App() {
  const [page, setPage] = useState('dashboard');
  const [tenants, setTenants] = useState([]);
  const [selectedTenantId, setSelectedTenantId] = useState('');
  const [apiKey, setApiKey] = useState(localStorage.getItem('rl_api_key') || '');
  const [refreshToken, setRefreshToken] = useState(0);
  const [error, setError] = useState('');

  useEffect(() => {
    listTenants()
      .then((data) => {
        setTenants(data);
        setSelectedTenantId((current) => current || data[0]?.id || '');
        setError('');
      })
      .catch((err) => setError(err.response?.data?.message || err.message));
  }, [refreshToken]);

  useEffect(() => {
    if (apiKey) localStorage.setItem('rl_api_key', apiKey);
  }, [apiKey]);

  const selectedTenant = useMemo(
    () => tenants.find((tenant) => tenant.id === selectedTenantId),
    [tenants, selectedTenantId],
  );

  const pageProps = { tenants, selectedTenant, selectedTenantId, setSelectedTenantId, apiKey, setApiKey, reloadTenants: () => setRefreshToken((v) => v + 1) };

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <aside className="fixed inset-y-0 left-0 hidden w-72 border-r border-slate-200 bg-slate-950 px-5 py-6 text-white lg:block">
        <div className="flex items-center gap-3">
          <div className="rounded-2xl bg-indigo-500 p-3 text-2xl">📡</div>
          <div>
            <p className="text-lg font-bold">Rate Limiter</p>
            <p className="text-xs text-slate-400">Operations Console</p>
          </div>
        </div>
        <nav className="mt-10 space-y-2">
          {nav.map((item) => {
            return (
              <button key={item.id} onClick={() => setPage(item.id)} className={`flex w-full items-center gap-3 rounded-2xl px-4 py-3 text-left text-sm font-semibold transition ${page === item.id ? 'bg-white text-slate-950' : 'text-slate-300 hover:bg-slate-900 hover:text-white'}`}>
                <span>{item.icon}</span> {item.label}
              </button>
            );
          })}
        </nav>
        <div className="absolute bottom-6 left-5 right-5 rounded-2xl bg-slate-900 p-4 text-xs text-slate-300">
          <div className="mb-2 text-lg text-emerald-400">●</div>
          Live decisions stream through <span className="font-semibold text-white">/ws/analytics</span>.
        </div>
      </aside>

      <main className="lg:pl-72">
        <header className="sticky top-0 z-10 border-b border-slate-200 bg-white/90 px-6 py-4 backdrop-blur">
          <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
            <div>
              <p className="text-sm font-semibold text-indigo-600">Distributed Rate Limiter as a Service</p>
              <h1 className="text-2xl font-bold">{nav.find((item) => item.id === page)?.label}</h1>
            </div>
            <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
              <label className="block min-w-72">
                <span className="label">Tenant</span>
                <select className="input mt-1" value={selectedTenantId} onChange={(e) => setSelectedTenantId(e.target.value)}>
                  <option value="">No tenant selected</option>
                  {tenants.map((tenant) => <option key={tenant.id} value={tenant.id}>{tenant.name} · {tenant.plan}</option>)}
                </select>
              </label>
              <label className="block min-w-80">
                <span className="label">API key for checks</span>
                <input className="input mt-1" value={apiKey} onChange={(e) => setApiKey(e.target.value)} placeholder="rl_..." />
              </label>
            </div>
          </div>
          {error && <p className="mt-3 rounded-xl bg-red-50 px-4 py-2 text-sm text-red-700">{error}</p>}
        </header>
        <div className="p-6">
          {page === 'dashboard' && <DashboardPage {...pageProps} />}
          {page === 'rules' && <RulesPage {...pageProps} />}
          {page === 'analytics' && <AnalyticsPage {...pageProps} />}
        </div>
      </main>
    </div>
  );
}

createRoot(document.getElementById('root')).render(<App />);
