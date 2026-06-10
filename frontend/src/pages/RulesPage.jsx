import { useEffect, useState } from 'react';
import { createRule, createTenant, deleteRule, listRules } from '../services/api.js';

const defaultRule = { scope: 'PER_IP', algorithm: 'TOKEN_BUCKET', limit: 100, windowSizeMs: 60000, endpoint: '', failStrategy: 'FAIL_OPEN' };

export function RulesPage({ selectedTenantId, setSelectedTenantId, setApiKey, reloadTenants }) {
  const [tenantForm, setTenantForm] = useState({ name: '', plan: 'FREE' });
  const [createdKey, setCreatedKey] = useState('');
  const [rules, setRules] = useState([]);
  const [ruleForm, setRuleForm] = useState(defaultRule);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!selectedTenantId) {
      setRules([]);
      return;
    }
    listRules(selectedTenantId).then(setRules).catch((err) => setError(err.response?.data?.message || err.message));
  }, [selectedTenantId]);

  const submitTenant = async (event) => {
    event.preventDefault();
    setError('');
    try {
      const response = await createTenant(tenantForm);
      setCreatedKey(response.apiKey);
      setApiKey(response.apiKey);
      setSelectedTenantId(response.tenant.id);
      setTenantForm({ name: '', plan: 'FREE' });
      reloadTenants();
    } catch (err) {
      setError(err.response?.data?.message || err.message);
    }
  };

  const submitRule = async (event) => {
    event.preventDefault();
    if (!selectedTenantId) return;
    setError('');
    try {
      const payload = { ...ruleForm, limit: Number(ruleForm.limit), windowSizeMs: Number(ruleForm.windowSizeMs), endpoint: ruleForm.endpoint || null };
      const created = await createRule(selectedTenantId, payload);
      setRules((current) => [...current, created]);
      setRuleForm(defaultRule);
    } catch (err) {
      setError(err.response?.data?.message || err.message);
    }
  };

  const removeRule = async (ruleId) => {
    await deleteRule(selectedTenantId, ruleId);
    setRules((current) => current.filter((rule) => rule.id !== ruleId));
  };

  return (
    <div className="grid gap-6 xl:grid-cols-[420px_1fr]">
      <div className="space-y-6">
        <form onSubmit={submitTenant} className="card space-y-4">
          <h2 className="text-lg font-bold">Create tenant</h2>
          <label><span className="label">Tenant name</span><input className="input mt-1" value={tenantForm.name} onChange={(e) => setTenantForm({ ...tenantForm, name: e.target.value })} placeholder="Acme API" required /></label>
          <label><span className="label">Plan</span><select className="input mt-1" value={tenantForm.plan} onChange={(e) => setTenantForm({ ...tenantForm, plan: e.target.value })}><option>FREE</option><option>PRO</option><option>ENTERPRISE</option></select></label>
          <button className="btn-primary">Create tenant</button>
          {createdKey && <div className="rounded-xl bg-emerald-50 p-3 text-sm text-emerald-800"><p className="font-semibold">One-time API key</p><code className="mt-2 block break-all">{createdKey}</code></div>}
        </form>

        <form onSubmit={submitRule} className="card space-y-4">
          <h2 className="text-lg font-bold">Add rule</h2>
          <div className="grid gap-3 sm:grid-cols-2">
            <label><span className="label">Scope</span><select className="input mt-1" value={ruleForm.scope} onChange={(e) => setRuleForm({ ...ruleForm, scope: e.target.value })}><option>PER_IP</option><option>PER_USER</option><option>GLOBAL</option></select></label>
            <label><span className="label">Algorithm</span><select className="input mt-1" value={ruleForm.algorithm} onChange={(e) => setRuleForm({ ...ruleForm, algorithm: e.target.value })}><option>TOKEN_BUCKET</option><option>FIXED_WINDOW</option><option>SLIDING_WINDOW_LOG</option><option>SLIDING_WINDOW_COUNTER</option></select></label>
          </div>
          <div className="grid gap-3 sm:grid-cols-2">
            <label><span className="label">Limit</span><input className="input mt-1" type="number" min="1" value={ruleForm.limit} onChange={(e) => setRuleForm({ ...ruleForm, limit: e.target.value })} /></label>
            <label><span className="label">Window (ms)</span><input className="input mt-1" type="number" min="1" value={ruleForm.windowSizeMs} onChange={(e) => setRuleForm({ ...ruleForm, windowSizeMs: e.target.value })} /></label>
          </div>
          <label><span className="label">Endpoint override</span><input className="input mt-1" value={ruleForm.endpoint} onChange={(e) => setRuleForm({ ...ruleForm, endpoint: e.target.value })} placeholder="Blank applies to all endpoints" /></label>
          <label><span className="label">Fail strategy</span><select className="input mt-1" value={ruleForm.failStrategy} onChange={(e) => setRuleForm({ ...ruleForm, failStrategy: e.target.value })}><option>FAIL_OPEN</option><option>FAIL_CLOSE</option></select></label>
          <button className="btn-primary" disabled={!selectedTenantId}>Create rule</button>
        </form>
      </div>

      <div className="card">
        <div className="mb-4 flex items-center justify-between"><h2 className="text-lg font-bold">Tenant rules</h2><span className="text-sm text-slate-500">{rules.length} configured</span></div>
        {error && <p className="mb-4 rounded-xl bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}
        <div className="overflow-hidden rounded-2xl border border-slate-200">
          <table className="min-w-full divide-y divide-slate-200 text-sm">
            <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500"><tr><th className="px-4 py-3">Scope</th><th className="px-4 py-3">Algorithm</th><th className="px-4 py-3">Limit</th><th className="px-4 py-3">Window</th><th className="px-4 py-3">Endpoint</th><th className="px-4 py-3"></th></tr></thead>
            <tbody className="divide-y divide-slate-100 bg-white">
              {rules.map((rule) => <tr key={rule.id}><td className="px-4 py-3 font-medium">{rule.scope}</td><td className="px-4 py-3 text-slate-600">{rule.algorithm}</td><td className="px-4 py-3">{rule.limit}</td><td className="px-4 py-3">{rule.windowSizeMs} ms</td><td className="px-4 py-3 text-slate-600">{rule.endpoint || '*'}</td><td className="px-4 py-3 text-right"><button className="text-sm font-semibold text-rose-600 hover:text-rose-700" onClick={() => removeRule(rule.id)}>Delete</button></td></tr>)}
              {rules.length === 0 && <tr><td colSpan="6" className="px-4 py-10 text-center text-slate-500">No rules yet for this tenant.</td></tr>}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
