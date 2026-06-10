import { useEffect, useMemo, useState } from 'react';
import { Area, AreaChart, Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { EventsTable } from '../components/EventsTable.jsx';
import { MetricCard } from '../components/MetricCard.jsx';
import { analyticsEvents, analyticsSummary } from '../services/api.js';

export function AnalyticsPage({ selectedTenantId }) {
  const [rangeHours, setRangeHours] = useState(24);
  const [summary, setSummary] = useState(null);
  const [events, setEvents] = useState([]);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!selectedTenantId) return;
    const since = new Date(Date.now() - rangeHours * 60 * 60 * 1000).toISOString();
    Promise.all([analyticsSummary(selectedTenantId, since), analyticsEvents(selectedTenantId)])
      .then(([summaryData, eventsData]) => { setSummary(summaryData); setEvents(eventsData); setError(''); })
      .catch((err) => setError(err.response?.data?.message || err.message));
  }, [selectedTenantId, rangeHours]);

  const timeline = useMemo(() => {
    const buckets = new Map();
    events.forEach((event) => {
      const d = new Date(event.eventTimestamp);
      d.setMinutes(0, 0, 0);
      const key = d.toISOString();
      const bucket = buckets.get(key) || { time: d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }), allowed: 0, rejected: 0 };
      event.allowed ? bucket.allowed++ : bucket.rejected++;
      buckets.set(key, bucket);
    });
    return [...buckets.entries()].sort(([a], [b]) => a.localeCompare(b)).map(([, value]) => value);
  }, [events]);

  const algorithms = useMemo(() => {
    const counts = new Map();
    events.forEach((event) => counts.set(event.algorithm, (counts.get(event.algorithm) || 0) + 1));
    return [...counts.entries()].map(([algorithm, count]) => ({ algorithm, count }));
  }, [events]);

  if (!selectedTenantId) return <div className="card text-center text-slate-600">Select or create a tenant to view analytics.</div>;

  return (
    <div className="space-y-6">
      <div className="flex justify-end"><select className="input max-w-48" value={rangeHours} onChange={(e) => setRangeHours(Number(e.target.value))}><option value="1">Last hour</option><option value="24">Last 24 hours</option><option value="168">Last 7 days</option></select></div>
      {error && <p className="rounded-xl bg-red-50 px-4 py-2 text-sm text-red-700">{error}</p>}
      <div className="grid gap-4 md:grid-cols-3"><MetricCard label="Requests" value={summary?.totalRequests ?? '—'} helper={`${rangeHours}h`} /><MetricCard label="Allowed" value={summary?.allowedRequests ?? '—'} helper="Success" tone="emerald" /><MetricCard label="Rejected" value={summary?.rejectedRequests ?? '—'} helper="Limited" tone="rose" /></div>
      <div className="grid gap-6 xl:grid-cols-2">
        <div className="card h-96"><h2 className="mb-4 text-lg font-bold">Hourly traffic</h2><ResponsiveContainer width="100%" height="85%"><AreaChart data={timeline}><CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="time" /><YAxis allowDecimals={false} /><Tooltip /><Area type="monotone" dataKey="allowed" stackId="1" stroke="#10b981" fill="#bbf7d0" /><Area type="monotone" dataKey="rejected" stackId="1" stroke="#f43f5e" fill="#fecdd3" /></AreaChart></ResponsiveContainer></div>
        <div className="card h-96"><h2 className="mb-4 text-lg font-bold">Algorithm usage</h2><ResponsiveContainer width="100%" height="85%"><BarChart data={algorithms}><CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="algorithm" /><YAxis allowDecimals={false} /><Tooltip /><Bar dataKey="count" fill="#4f46e5" radius={[8, 8, 0, 0]} /></BarChart></ResponsiveContainer></div>
      </div>
      <div className="card"><h2 className="mb-4 text-lg font-bold">Recent analytics events</h2><EventsTable events={events} /></div>
    </div>
  );
}
