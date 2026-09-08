import { useState, useEffect, useCallback } from 'react';
import './index.css';

const API_BASE = 'http://localhost:8080';

const generateSLA = (id) => {
  const hash = id.split('').reduce((a, b) => a + b.charCodeAt(0), 0);
  const minutes = hash % 120;
  if (minutes < 15) return { minutes, status: 'red' };
  if (minutes < 45) return { minutes, status: 'amber' };
  return { minutes, status: 'green' };
};

function App() {
  const [agents, setAgents] = useState([]);
  const [orders, setOrders] = useState([]);
  const [suggestions, setSuggestions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [streamingStreams, setStreamingStreams] = useState({});
  const [newOrderDesc, setNewOrderDesc] = useState('');
  const [newOrderAgent, setNewOrderAgent] = useState('');
  const [isCreating, setIsCreating] = useState(false);
  const [routingStrategy, setRoutingStrategy] = useState('ruleBased');

  const fetchData = useCallback(async (silent = false) => {
    if (!silent) setLoading(true);
    setError(null);
    try {
      const [agentsRes, ordersRes, suggestionsRes, routingRes] = await Promise.all([
        fetch(`${API_BASE}/agents`),
        fetch(`${API_BASE}/orders`),
        fetch(`${API_BASE}/suggestions`),
        fetch(`${API_BASE}/routing/strategy`),
      ]);

      if (!agentsRes.ok || !ordersRes.ok || !suggestionsRes.ok) {
        throw new Error('Backend returned an error. Is the server running on port 8080?');
      }

      setAgents(await agentsRes.json());
      setOrders(await ordersRes.json());
      setSuggestions(await suggestionsRes.json());
      if (routingRes.ok) {
        const routing = await routingRes.json();
        setRoutingStrategy(routing.activeStrategy);
      }
    } catch (err) {
      setError(err.message || 'Failed to connect to backend');
    } finally {
      if (!silent) setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
    const interval = setInterval(() => fetchData(true), 5000);
    return () => clearInterval(interval);
  }, [fetchData]);

  const handleAction = async (suggestionId, action) => {
    try {
      const res = await fetch(`${API_BASE}/suggestions/${suggestionId}`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status: action === 'accept' ? 'ACCEPTED' : 'REJECTED' }),
      });
      if (!res.ok) throw new Error('Failed to update suggestion');
      fetchData(true);
    } catch (err) {
      setError(err.message);
    }
  };

  const simulateAgentOffline = async (agentId) => {
    try {
      const res = await fetch(`${API_BASE}/agents/${agentId}/status`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status: 'OFFLINE' }),
      });
      if (!res.ok) throw new Error('Failed to set agent offline');
      setTimeout(() => fetchData(true), 1500);
    } catch (err) {
      setError(err.message);
    }
  };

  const handleCreateOrder = async (e) => {
    e.preventDefault();
    if (!newOrderDesc || !newOrderAgent) return;

    setIsCreating(true);
    try {
      const res = await fetch(`${API_BASE}/orders`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          description: newOrderDesc,
          assignedAgentId: newOrderAgent,
        }),
      });
      if (!res.ok) throw new Error('Failed to create order');
      setNewOrderDesc('');
      setNewOrderAgent('');
      fetchData(true);
    } catch (err) {
      setError(err.message);
    } finally {
      setIsCreating(false);
    }
  };

  const startStream = (orderId) => {
    setStreamingStreams((prev) => ({
      ...prev,
      [orderId]: { text: '', active: true },
    }));

    const eventSource = new EventSource(`${API_BASE}/orders/${orderId}/suggest/stream`);

    eventSource.onmessage = (event) => {
      setStreamingStreams((prev) => ({
        ...prev,
        [orderId]: { ...prev[orderId], text: (prev[orderId]?.text || '') + event.data },
      }));
    };

    eventSource.addEventListener('complete', () => {
      eventSource.close();
      setStreamingStreams((prev) => {
        const next = { ...prev };
        delete next[orderId];
        return next;
      });
      fetchData(true);
    });

    eventSource.onerror = () => {
      eventSource.close();
      setStreamingStreams((prev) => {
        const next = { ...prev };
        delete next[orderId];
        return next;
      });
      setError('AI stream failed. Try again or use manual refresh.');
    };
  };

  const pendingOrders = orders.filter((o) => o.status === 'REASSIGNMENT_PENDING');

  return (
    <div className="dashboard">
      <header className="header">
        <h1>
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"></polygon>
          </svg>
          ZipRun Ops Center
        </h1>
        <button className="refresh-btn" onClick={() => fetchData()} disabled={loading}>
          {loading ? 'SYNCING...' : 'LIVE SYNC'}
        </button>
        <span className="strategy-badge" title="Active routing strategy">
          Strategy: {routingStrategy}
        </span>
      </header>

      {error && (
        <div className="error-banner" role="alert">
          {error}
          <button onClick={() => setError(null)} aria-label="Dismiss">×</button>
        </div>
      )}

      <div className="main-grid">
        <div className="panel">
          <div className="panel-header">
            <span>Agent Roster</span>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
              {agents.filter((a) => a.status !== 'OFFLINE').length} Active
            </span>
          </div>
          <div className="panel-content">
            {agents.map((agent) => {
              const maxLoad = agent.maxCapacity || 5;
              const loadPercent = Math.min(100, (agent.activeOrderCount / maxLoad) * 100);
              const isOverloaded = loadPercent >= 80;

              return (
                <div key={agent.id} className="list-item">
                  <div className="agent-header">
                    <div>
                      <div className="agent-name">{agent.name}</div>
                      <div className="agent-id">{agent.id}</div>
                    </div>
                    <span className={`status-badge status-${agent.status.toLowerCase()}`}>
                      {agent.status}
                    </span>
                  </div>
                  <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                    Load: {agent.activeOrderCount}/{maxLoad} orders
                  </div>
                  <div className="capacity-bar-container">
                    <div
                      className="capacity-bar"
                      style={{
                        width: `${loadPercent}%`,
                        background:
                          agent.status === 'OFFLINE'
                            ? 'var(--status-offline-text)'
                            : isOverloaded
                              ? 'var(--warning-color)'
                              : 'var(--accent-color)',
                      }}
                    />
                  </div>
                  {agent.status !== 'OFFLINE' && (
                    <button className="crash-btn" onClick={() => simulateAgentOffline(agent.id)}>
                      Simulate Crash
                    </button>
                  )}
                </div>
              );
            })}
          </div>
        </div>

        <div className="panel">
          <div className="panel-header">
            <span>Dispatch Board</span>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>{orders.length} Orders</span>
          </div>
          <div className="panel-content">
            <div style={{ padding: '1rem', borderBottom: '1px solid var(--panel-border)', background: 'rgba(255,255,255,0.02)' }}>
              <form onSubmit={handleCreateOrder} style={{ display: 'flex', gap: '0.5rem' }}>
                <input
                  type="text"
                  placeholder="Order Description..."
                  value={newOrderDesc}
                  onChange={(e) => setNewOrderDesc(e.target.value)}
                  style={{ flex: 1, padding: '0.5rem', borderRadius: '4px', border: '1px solid var(--panel-border)', background: 'rgba(0,0,0,0.2)', color: 'white' }}
                />
                <select
                  value={newOrderAgent}
                  onChange={(e) => setNewOrderAgent(e.target.value)}
                  style={{ padding: '0.5rem', borderRadius: '4px', border: '1px solid var(--panel-border)', background: 'rgba(0,0,0,0.2)', color: 'white' }}
                >
                  <option value="">Select Agent...</option>
                  {agents.filter((a) => a.status !== 'OFFLINE').map((a) => (
                    <option key={a.id} value={a.id}>{a.name}</option>
                  ))}
                </select>
                <button type="submit" disabled={isCreating || !newOrderDesc || !newOrderAgent} className="refresh-btn" style={{ padding: '0.5rem 1rem' }}>
                  {isCreating ? '...' : '+ Add'}
                </button>
              </form>
            </div>

            {orders.length === 0 ? (
              <div className="empty-state">
                <div className="empty-icon">📦</div>
                <p>No orders</p>
              </div>
            ) : (
              orders.map((order) => {
                const sla = generateSLA(order.id);
                const isStreamActive = streamingStreams[order.id]?.active;

                return (
                  <div key={order.id} className="list-item">
                    <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                      <div>
                        <div className="order-title">{order.id}</div>
                        <div className="order-desc">{order.description}</div>
                        <div className="sla-indicator">
                          <div className={`sla-dot sla-${sla.status}`} />
                          <span>SLA: {sla.minutes}m remaining</span>
                        </div>
                      </div>
                      <div style={{ textAlign: 'right' }}>
                        <div style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-muted)' }}>
                          {order.status}
                        </div>
                        {order.assignedAgent && (
                          <div style={{ fontSize: '0.8rem', marginTop: '4px' }}>
                            Agent: {order.assignedAgent.name}
                          </div>
                        )}
                        {order.status === 'ASSIGNED' && (
                          <button className="reassign-btn" onClick={() => startStream(order.id)} disabled={isStreamActive}>
                            {isStreamActive ? 'THINKING...' : '✨ AI Reassign'}
                          </button>
                        )}
                      </div>
                    </div>

                    {isStreamActive && (
                      <div className="ai-reasoning ai-streaming" style={{ marginTop: '1rem' }}>
                        <strong style={{ color: 'var(--accent-color)', display: 'flex', alignItems: 'center', gap: '8px' }}>
                          <div className="sla-dot sla-amber" style={{ animation: 'blink 1s infinite' }} />
                          AI Reasoning Stream
                        </strong>
                        <div style={{ minHeight: '40px' }}>
                          {streamingStreams[order.id]?.text}
                          <span className="typing-cursor" />
                        </div>
                      </div>
                    )}
                  </div>
                );
              })
            )}
          </div>
        </div>

        <div className="panel">
          <div className="panel-header">
            <span>Reassignment Queue</span>
            {suggestions.length > 0 && (
              <span className="status-badge status-busy">{suggestions.length} Pending</span>
            )}
          </div>
          <div className="panel-content">
            {pendingOrders.length > 0 && suggestions.length === 0 && (
              <div className="empty-state">
                <p>{pendingOrders.length} order(s) awaiting suggestion...</p>
              </div>
            )}
            {suggestions.length === 0 ? (
              <div className="empty-state">
                <div className="empty-icon">✓</div>
                <p>Queue is empty.</p>
                <p style={{ fontSize: '0.85em', marginTop: '8px' }}>
                  Click &quot;Simulate Crash&quot; on an agent to trigger the agentic re-plan loop.
                </p>
              </div>
            ) : (
              suggestions.map((s) => (
                <div key={s.id} className="suggestion-card">
                  <div className="suggestion-header">
                    <div>
                      <div className="order-title">{s.order.id}</div>
                      <div className="order-desc">{s.order.description}</div>
                    </div>
                    {s.triggerReason === 'AGENT_OFFLINE' ? (
                      <span className="trigger-badge">⚡ AGENTIC REPLAN</span>
                    ) : (
                      <span className="trigger-badge manual">📋 MANUAL</span>
                    )}
                  </div>

                  <div className="ai-reasoning">
                    <strong>
                      Recommended: {s.recommendedAgent.name} ({s.recommendedAgent.id})
                    </strong>
                    <p style={{ marginTop: '0.5rem' }}>{s.reasoning}</p>

                    <div style={{ marginTop: '1rem', display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                      <span>Confidence Score</span>
                      <span>{((s.confidenceScore ?? 0) * 100).toFixed(0)}%</span>
                    </div>
                    <div className="confidence-bar-bg">
                      <div className="confidence-bar-fill" style={{ width: `${(s.confidenceScore ?? 0) * 100}%` }} />
                    </div>
                  </div>

                  <div className="actions">
                    <button className="btn btn-accept" onClick={() => handleAction(s.id, 'accept')}>
                      ✓ Accept
                    </button>
                    <button className="btn btn-reject" onClick={() => handleAction(s.id, 'reject')}>
                      ✕ Reject
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

export default App;
