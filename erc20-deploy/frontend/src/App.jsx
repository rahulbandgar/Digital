import { useState } from 'react';

export default function App() {
  const [loading, setLoading] = useState(false);
  const [contractAddress, setContractAddress] = useState(null);
  const [error, setError] = useState(null);

  async function handleDeploy() {
    setLoading(true);
    setError(null);
    setContractAddress(null);

    try {
      const response = await fetch('/api/deploy', { method: 'POST' });
      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.error || 'Deployment failed');
      }
      setContractAddress(data.contractAddress);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="page">
      <div className="card">
        <h1>ERC20 Token Deployer</h1>
        <p className="subtitle">Deploys SimpleToken (STK) to Besu network</p>

        <div className="deploy-row">
          <button
            className="deploy-btn"
            onClick={handleDeploy}
            disabled={loading}
          >
            {loading ? (
              <span className="spinner-wrap">
                <span className="spinner" />
                Deploying…
              </span>
            ) : (
              'Hi'
            )}
          </button>

          {contractAddress && (
            <div className="address-badge">
              <span className="address-label">Contract:</span>
              <code className="address-value">{contractAddress}</code>
            </div>
          )}
        </div>

        {error && <div className="error-box">{error}</div>}
      </div>
    </div>
  );
}
