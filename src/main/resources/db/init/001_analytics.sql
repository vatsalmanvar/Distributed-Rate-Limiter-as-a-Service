CREATE TABLE IF NOT EXISTS rate_limit_analytics (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL,
    identifier VARCHAR(255),
    endpoint VARCHAR(500),
    allowed BOOLEAN,
    algorithm VARCHAR(50),
    event_timestamp TIMESTAMPTZ,
    remaining_tokens BIGINT
);

CREATE INDEX IF NOT EXISTS idx_tenant_time
    ON rate_limit_analytics (tenant_id, event_timestamp DESC);
