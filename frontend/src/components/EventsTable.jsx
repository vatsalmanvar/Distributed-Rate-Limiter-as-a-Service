export function EventsTable({ events }) {
  return (
    <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white">
      <table className="min-w-full divide-y divide-slate-200 text-sm">
        <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500">
          <tr>
            <th className="px-4 py-3">Time</th>
            <th className="px-4 py-3">Identifier</th>
            <th className="px-4 py-3">Endpoint</th>
            <th className="px-4 py-3">Decision</th>
            <th className="px-4 py-3">Algorithm</th>
            <th className="px-4 py-3">Remaining</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {events.map((event, index) => (
            <tr key={event.id || `${event.timestamp}-${index}`} className="hover:bg-slate-50">
              <td className="whitespace-nowrap px-4 py-3 text-slate-600">{formatTime(event.eventTimestamp || event.timestamp)}</td>
              <td className="px-4 py-3 font-medium">{event.identifier}</td>
              <td className="px-4 py-3 text-slate-600">{event.endpoint || '*'}</td>
              <td className="px-4 py-3"><Decision allowed={event.allowed} /></td>
              <td className="px-4 py-3 text-slate-600">{event.algorithm}</td>
              <td className="px-4 py-3 text-slate-600">{event.remainingTokens ?? event.remaining}</td>
            </tr>
          ))}
          {events.length === 0 && (
            <tr><td colSpan="6" className="px-4 py-10 text-center text-slate-500">No events yet. Run a rate limit check to populate analytics.</td></tr>
          )}
        </tbody>
      </table>
    </div>
  );
}

function Decision({ allowed }) {
  return <span className={`rounded-full px-3 py-1 text-xs font-semibold ${allowed ? 'bg-emerald-50 text-emerald-700' : 'bg-rose-50 text-rose-700'}`}>{allowed ? 'Allowed' : 'Rejected'}</span>;
}

function formatTime(value) {
  if (!value) return '—';
  const date = typeof value === 'number' ? new Date(value) : new Date(value);
  return date.toLocaleString();
}
