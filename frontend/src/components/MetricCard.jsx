export function MetricCard({ label, value, helper, tone = 'indigo' }) {
  const tones = {
    indigo: 'bg-indigo-50 text-indigo-700',
    emerald: 'bg-emerald-50 text-emerald-700',
    rose: 'bg-rose-50 text-rose-700',
    amber: 'bg-amber-50 text-amber-700',
  };
  return (
    <div className="card">
      <p className="label">{label}</p>
      <div className="mt-3 flex items-end justify-between gap-4">
        <p className="text-3xl font-bold">{value}</p>
        <span className={`rounded-full px-3 py-1 text-xs font-semibold ${tones[tone]}`}>{helper}</span>
      </div>
    </div>
  );
}
