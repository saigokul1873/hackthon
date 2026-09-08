import { useState, useEffect } from 'react';
import './index.css';

const API_BASE = 'http://localhost:8080';

// Fake SLA data logic for ceiling requirement
const generateSLA = (id) => {
  const hash = id.split('').reduce((a, b) => a + b.charCodeAt(0), 0);
  const minutes = (hash % 120);
  if (minutes < 15) return { minutes, status: 'red' };
  if (minutes < 45) return { minutes, status: 'amber' };
  return { minutes, status: 'green' };
};

function App() {
  const [agents, setAgents] = useState([]);
  const [orders, setOrders] = useState([]);
  const [suggestions, setSuggestions] = useState([]);
  const [loading, setLoading] = useState(true);
  
  // Streaming state
  const [streamingStreams, setStreamingStreams] = useState({});
  
  // Create Order state
  const [newOrderDesc, setNewOrderDesc] = useState('');
  const [newOrderAgent, setNewOrderAgent] = useState('');
  const [isCreating, setIsCreating] = useState(false);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [agentsRes, ordersRes, suggestionsRes] = await Promise.all([
        fetch(`${API_BASE}/agents`),
        fetch(`${API_BASE}/orders`),
        fetch(`${API_BASE}/suggestions`)
      ]);
      
      const agentsData = await agentsRes.json();
      const ordersData = await ordersRes.json();
      const suggestionsData = await suggestionsRes.json();
      
      setAgents(agentsData);
      setOrders(ordersData);
      setSuggestions(suggestionsData);
    } catch (error) {
      console.error("Failed to fetch data", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
    const interval = setInterval(fetchData, 5000);
    return () => clearInterval(interval);
  }, []);

  const handleAction = async (suggestionId, action) => {
    try {
      await fetch(`${API_BASE}/suggestions/${suggestionId}`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status: action === 'accept' ? 'ACCEPTED' : 'REJECTED' })
      });
      fetchData();
    } catch (error) {
      console.error("Failed to process action", error);
    }
  };

  const simulateAgentOffline = async (agentId) => {
    try {
      await fetch(`${API_BASE}/agents/${agentId}/status`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status: 'OFFLINE' })
      });
      setTimeout(fetchData, 1000);
    } catch (error) {
      console.error("Failed to set agent offline", error);
    }
  };

  const handleCreateOrder = async (e) => {
    e.preventDefault();
    if (!newOrderDesc || !newOrderAgent) return;
    
    setIsCreating(true);
    try {
      await fetch(`${API_BASE}/orders`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          description: newOrderDesc,
          assignedAgentId: newOrderAgent
        })
      });
      setNewOrderDesc('');
      setNewOrderAgent('');
      fetchData();
    } catch (error) {
      console.error("Failed to create order", error);
    } finally {
      setIsCreating(false);
    }
  };

  const startStream = (orderId) => {
    // Initialize stream state for this order
    setStreamingStreams(prev => ({
      ...prev,
      [orderId]: { text: '', active: true, orderId }
    }));

    const eventSource = new EventSource(`${API_BASE}/orders/${orderId}/suggest/stream`, {
      method: 'POST'
    });

    eventSource.onmessage = (event) => {
      // Append word to the stream
      setStreamingStreams(prev => ({
        ...prev,
        [orderId]: { ...prev[orderId], text: prev[orderId].text + event.data }
      }));
    };

    eventSource.addEventListener('complete', (event) => {
      eventSource.close();
      setStreamingStreams(prev => {
        const next = {...prev};
        delete next[orderId];
        return next;
      });
      // Fetch new data to show the finalized suggestion
      fetchData();
    });

    eventSource.onerror = (error) => {
      console.error("EventSource failed:", error);
      eventSource.close();
      setStreamingStreams(prev => {
        const next = {...prev};
        delete next[orderId];
        return next;
      });
    };
  };

  return (
    <div className="dashboard">
      <header className="header">
        <h1>
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"></polygon>
          </svg>
          ZipRun Ops Center
        </h1>
        <button className="refresh-btn" onClick={fetchData} disabled={loading}>
          {loading ? 'SYNCING...' : 'LIVE SYNC'}
        </button>
      </header>

      <div className="main-grid">
        
        {/* Agent Roster */}
        <div className="panel">
          <div className="panel-header">
            <span>Agent Roster</span>
            <span style={{fontSize:'0.8rem', color:'var(--text-muted)'}}>{agents.length} Online</span>
          </div>
          <div className="panel-content">
            {agents.map(agent => {
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
                  <div style={{fontSize: '0.8rem', color: 'var(--text-muted)'}}>
                    Load: {agent.activeOrderCount}/{maxLoad} orders
                  </div>
                  <div className="capacity-bar-container">
                    <div 
                      className="capacity-bar" 
                      style={{
                        width: `${loadPercent}%`,
                        background: agent.status === 'OFFLINE' ? 'var(--status-offline-text)' : (isOverloaded ? 'var(--warning-color)' : 'var(--accent-color)')
                      }}
                    ></div>
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

        {/* Dispatch Board */}
        <div className="panel">
          <div className="panel-header">
            <span>Dispatch Board</span>
            <span style={{fontSize:'0.8rem', color:'var(--text-muted)'}}>{orders.length} Active</span>
          </div>
          <div className="panel-content">
            <div style={{padding: '1rem', borderBottom: '1px solid var(--panel-border)', background: 'rgba(255,255,255,0.02)'}}>
              <form onSubmit={handleCreateOrder} style={{display: 'flex', gap: '0.5rem'}}>
                <input 
                  type="text" 
                  placeholder="Order Description..." 
                  value={newOrderDesc}
                  onChange={e => setNewOrderDesc(e.target.value)}
                  style={{flex: 1, padding: '0.5rem', borderRadius: '4px', border: '1px solid var(--panel-border)', background: 'rgba(0,0,0,0.2)', color: 'white'}}
                />
                <select 
                  value={newOrderAgent} 
                  onChange={e => setNewOrderAgent(e.target.value)}
                  style={{padding: '0.5rem', borderRadius: '4px', border: '1px solid var(--panel-border)', background: 'rgba(0,0,0,0.2)', color: 'white'}}
                >
                  <option value="">Select Agent...</option>
                  {agents.filter(a => a.status !== 'OFFLINE').map(a => (
                    <option key={a.id} value={a.id}>{a.name}</option>
                  ))}
                </select>
                <button type="submit" disabled={isCreating || !newOrderDesc || !newOrderAgent} className="refresh-btn" style={{padding: '0.5rem 1rem'}}>
                  {isCreating ? '+' : '+ Add'}
                </button>
              </form>
            </div>
            
            {orders.length === 0 ? (
              <div className="empty-state">
                <div className="empty-icon">📦</div>
                <p>No active orders</p>
              </div>
            ) : (
              orders.map(order => {
                const sla = generateSLA(order.id);
                const isStreamActive = streamingStreams[order.id]?.active;
                
                return (
                  <div key={order.id} className="list-item">
                    <div style={{display: 'flex', justifyContent: 'space-between'}}>
                      <div>
                        <div className="order-title">{order.id}</div>
                        <div className="order-desc">{order.description}</div>
                        
                        <div className="sla-indicator">
                          <div className={`sla-dot sla-${sla.status}`}></div>
                          <span>SLA: {sla.minutes}m remaining</span>
                        </div>
                      </div>
                      <div style={{textAlign: 'right'}}>
                        <div style={{fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-muted)'}}>
                          {order.status}
                        </div>
                        {order.status === 'ASSIGNED' && (
                          <div style={{fontSize: '0.8rem', marginTop: '4px'}}>
                            Agent: {order.assignedAgent?.name || 'Unknown'}
                          </div>
                        )}
                        {order.status === 'ASSIGNED' && (
                          <button 
                            className="reassign-btn" 
                            onClick={() => startStream(order.id)}
                            disabled={isStreamActive}
                          >
                            {isStreamActive ? 'THINKING...' : '✨ AI Reassign'}
                          </button>
                        )}
                      </div>
                    </div>
                    
                    {/* Live Stream Box */}
                    {isStreamActive && (
                      <div className="ai-reasoning ai-streaming" style={{marginTop: '1rem'}}>
                        <strong style={{color: 'var(--accent-color)', display: 'flex', alignItems: 'center', gap: '8px'}}>
                          <div className="sla-dot sla-amber" style={{animation: 'blink 1s infinite'}}></div>
                          AI Reasoning Stream
                        </strong>
                        <div style={{minHeight: '40px'}}>
                          {streamingStreams[order.id].text}
                          <span className="typing-cursor"></span>
                        </div>
                      </div>
                    )}
                  </div>
                );
              })
            )}
          </div>
        </div>

        {/* Reassignment Queue */}
        <div className="panel">
          <div className="panel-header">
            <span>Reassignment Queue</span>
            {suggestions.length > 0 && (
              <span className="status-badge status-busy">{suggestions.length} Pending</span>
            )}
          </div>
          <div className="panel-content">
            {suggestions.length === 0 ? (
              <div className="empty-state">
                <div className="empty-icon">✓</div>
                <p>Queue is empty.</p>
                <p style={{fontSize: '0.85em', marginTop: '8px'}}>No pending actions required.</p>
              </div>
            ) : (
              suggestions.map(s => (
                <div key={s.id} className="suggestion-card">
                  <div className="suggestion-header">
                    <div>
                      <div className="order-title">{s.order.id}</div>
                      <div className="order-desc">{s.order.description}</div>
                    </div>
                    {s.triggerReason === 'AGENT_OFFLINE' && (
                      <span className="trigger-badge">⚡ AGENTIC REPLAN</span>
                    )}
                  </div>
                  
                  <div className="ai-reasoning">
                    <strong>Recommended: {s.recommendedAgent.name} ({s.recommendedAgent.id})</strong>
                    {s.reasoning}
                    
                    <div style={{marginTop: '1rem', display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', color: 'var(--text-muted)'}}>
                      <span>Confidence Score</span>
                      <span>{(s.confidenceScore * 100).toFixed(0)}%</span>
                    </div>
                    <div className="confidence-bar-bg">
                      <div className="confidence-bar-fill" style={{width: `${s.confidenceScore * 100}%`}}></div>
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
