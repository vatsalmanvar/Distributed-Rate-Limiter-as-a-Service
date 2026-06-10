import { useEffect, useMemo, useState } from 'react';
import { Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts';
import { EventsTable } from '../components/EventsTable.jsx';
import { MetricCard } from '../components/MetricCard.jsx';
import { analyticsEvents, analyticsSummary, checkRateLimit, wsUrl } from '../services/api.js';

export function DashboardPage({ selectedTenantId, selectedTenant, apiKey }) {
  const [summary, setSummary] = useState(null);
  const [events, setEvents] = useState([]);
  const [status, setStatus] = useState('Disconnected');
  const [checkForm, setCheckForm] = useState({ scope: 'PER_IP', identifier: '127.0.0.1', endpoint: '/api/orders' });
  const [checkResult, setCheckResult] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!selectedTenantId) return;
    analyticsSummary(selectedTenantId).then(setSummary).catch(() => {});
    analyticsEvents(selectedTenantId).then(setEvents).catch(() => {});
  }, [selectedTenantId]);

  useEffect(() => {
    if (!selectedTenantId) return;
    const socket = new WebSocket(wsUrl(selectedTenantId));
    socket.onopen = () => setStatus('Connected');
    socket.onclose = () => setStatus('Disconnected');
    socket.onerror = () => setStatus('Error');
    socket.onmessage = (message) => {
      const event = JSON.parse(message.data);
      setEvents((current) => [event, ...current].slice(0, 100));
      setSummary((current) => current ? {
        ...current,
        totalRequests: current.totalRequests + 1,
        allowedRequests: current.allowedRequests + (event.allowed ? 1 : 0),
        rejectedRequests: current.rejectedRequests + (event.allowed ? 0 : 1),
      } : current);
    };
    return () => socket.close();
  }, [selectedTenantId]);

  const chartData = useMemo(() => [
    { name: 'Allowed', value: summary?.allowedRequests || 0, color: '#10b981' },
    { name: 'Rejected', value: summary?.rejectedRequests || 0, color: '#f43f5e' },
  ], [summary]);

  const runCheck = async (event) => {
    event.preventDefault();
    setError('');
    try {
      const payload = { ...checkForm, identifier: checkForm.scope === 'GLOBAL' ? undefined : checkForm.identifier };
      const result = await checkRateLimit(apiKey, payload);
      setCheckResult(result);
    } catch (err) {
      setError(err.response?.data?.message || err.message);
    }
  };

  if (!selectedTenantId) return <EmptyState />;

  return (
    <div className="space-y-6">
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <MetricCard label="Tenant" value={selectedTenant?.name || '—'} helper={selectedTenant?.plan || 'Plan'} />
        <MetricCard label="Total requests" value={summary?.totalRequests ?? '—'} helper="24h" tone="amber" />
        <MetricCard label="Allowed" value={summary?.allowedRequests ?? '—'} helper="Passed" tone="emerald" />
        <MetricCard label="Rejected" value={summary?.rejectedRequests ?? '—'} helper={status} tone="rose" />
      </div>

      <div className="grid gap-6 xl:grid-cols-[1fr_420px]">
        <div className="card">
          <div className="mb-4 flex items-center justify-between"><h2 className="text-lg font-bold">Live request decisions</h2><span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-600">WebSocket: {status}</span></div>
          <EventsTable events={events.slice(0, 12)} />
        </div>
        <div className="space-y-6">
          <div className="card h-80">
            <h2 className="text-lg font-bold">Decision mix</h2>
            <ResponsiveContainer width="100%" height="85%">
              <PieChart>
                <Pie data={chartData} dataKey="value" nameKey="name" innerRadius={60} outerRadius={95} paddingAngle={5}>
                  {chartData.map((entry) => <Cell key={entry.name} fill={entry.color} />)}
                </Pie>
                <Tooltip />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          </div>
          <form onSubmit={runCheck} className="card space-y-4">
            <h2 className="text-lg font-bold">Try a rate limit check</h2>
            <div className="grid gap-3 sm:grid-cols-2">
              <label><span className="label">Scope</span><select className="input mt-1" value={checkForm.scope} onChange={(e) => setCheckForm({ ...checkForm, scope: e.target.value })}><option>PER_IP</option><option>PER_USER</option><option>GLOBAL</option></select></label>
              <label><span className="label">Identifier</span><input className="input mt-1" disabled={checkForm.scope === 'GLOBAL'} value={checkForm.identifier} onChange={(e) => setCheckForm({ ...checkForm, identifier: e.target.value })} /></label>
            </div>
            <label><span className="label">Endpoint</span><input className="input mt-1" value={checkForm.endpoint} onChange={(e) => setCheckForm({ ...checkForm, endpoint: e.target.value })} /></label>
            <button className="btn-primary" disabled={!apiKey}>Send check</button>
            {error && <p className="rounded-xl bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}
            {checkResult && <pre className="overflow-auto rounded-xl bg-slate-950 p-3 text-xs text-slate-100">{JSON.stringify(checkResult, null, 2)}</pre>}
          </form>
        </div>
      </div>
    </div>
  );
}

function EmptyState() {
  return <div className="card text-center"><h2 className="text-xl font-bold">Create a tenant to start</h2><p className="mt-2 text-slate-600">Open the Rules page, create a tenant, copy the one-time API key, and add rate limit rules.</p></div>;
}
