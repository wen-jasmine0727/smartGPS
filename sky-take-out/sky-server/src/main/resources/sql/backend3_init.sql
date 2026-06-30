-- Backend 3: alerts / commands / devices / knowledge
-- Run this after backend1_business_init.sql
-- psql -U postgres -d smart_logistics -f backend3_init.sql

-- pgvector 扩展（RAG 智能问答依赖）
CREATE EXTENSION IF NOT EXISTS vector;

-- 1. alerts 告警表
DROP TABLE IF EXISTS alert_logs;
DROP TABLE IF EXISTS alerts;

CREATE TABLE alerts (
  alert_id VARCHAR(64) PRIMARY KEY,
  alert_type VARCHAR(32) NOT NULL,
  severity VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  vehicle_plate VARCHAR(32),
  cargo_id VARCHAR(64),
  title VARCHAR(128),
  summary TEXT,
  description TEXT,
  lat DOUBLE PRECISION,
  lng DOUBLE PRECISION,
  triggered_at TIMESTAMPTZ NOT NULL,
  acknowledged_at TIMESTAMPTZ,
  resolved_at TIMESTAMPTZ,
  resolution VARCHAR(64),
  remark TEXT
);

CREATE INDEX idx_alerts_status ON alerts(status);
CREATE INDEX idx_alerts_vehicle ON alerts(vehicle_plate);
CREATE INDEX idx_alerts_triggered ON alerts(triggered_at DESC);

-- 2. alert_logs 告警处理日志
CREATE TABLE alert_logs (
  id VARCHAR(64) PRIMARY KEY,
  alert_id VARCHAR(64) NOT NULL REFERENCES alerts(alert_id),
  operator_id VARCHAR(32),
  operator_name VARCHAR(64),
  action VARCHAR(64) NOT NULL,
  remark TEXT,
  created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_alert_logs_alert ON alert_logs(alert_id, created_at DESC);

-- 3. commands 调度指令表
DROP TABLE IF EXISTS command_logs;
DROP TABLE IF EXISTS commands;

CREATE TABLE commands (
  command_id VARCHAR(64) PRIMARY KEY,
  plate VARCHAR(32) NOT NULL,
  vin_topic VARCHAR(64) NOT NULL,
  command_type VARCHAR(32) NOT NULL,
  priority VARCHAR(32) NOT NULL,
  payload JSONB,
  status VARCHAR(32) DEFAULT 'SENT',
  mqtt_topic VARCHAR(128),
  created_by VARCHAR(32),
  created_at TIMESTAMPTZ DEFAULT now(),
  updated_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_commands_plate ON commands(plate, created_at DESC);

-- 4. command_logs 指令状态流转日志
CREATE TABLE command_logs (
  id VARCHAR(64) PRIMARY KEY,
  command_id VARCHAR(64) NOT NULL REFERENCES commands(command_id),
  status VARCHAR(32) NOT NULL,
  source VARCHAR(32),
  raw_payload JSONB,
  created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_command_logs_cmd ON command_logs(command_id, created_at DESC);

-- 5. knowledge_documents 知识库文档
DROP TABLE IF EXISTS knowledge_chunks;
DROP TABLE IF EXISTS knowledge_documents;

CREATE TABLE knowledge_documents (
  document_id VARCHAR(64) PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  category VARCHAR(64),
  object_key VARCHAR(512),
  status VARCHAR(32) DEFAULT 'UPLOADED',
  chunk_count INT DEFAULT 0,
  created_at TIMESTAMPTZ DEFAULT now()
);

-- 6. knowledge_chunks 文档切片 + 向量
CREATE TABLE knowledge_chunks (
  chunk_id VARCHAR(64) PRIMARY KEY,
  document_id VARCHAR(64) REFERENCES knowledge_documents(document_id),
  chunk_index INT,
  content TEXT,
  embedding VECTOR(1024),
  created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_knowledge_chunks_embedding ON knowledge_chunks USING ivfflat (embedding vector_cosine_ops);
CREATE INDEX idx_knowledge_chunks_doc ON knowledge_chunks(document_id);

-- 7. 演示数据（可选，方便联调）
INSERT INTO alerts (
  alert_id, alert_type, severity, status,
  vehicle_plate, cargo_id,
  title, summary, description,
  lat, lng, triggered_at
) VALUES (
  'ALT-20260629-001', 'ROUTE_DEVIATION', 'WARNING', 'PENDING',
  '沪A·C0291', 'SH-HZ-20260629-0291',
  '偏航告警', '偏离 G60 沪昆高速预设路线 3.2km',
  '车辆偏离 G60 沪昆高速预设路线，当前位于 G320 国道海宁段。',
  30.4219, 120.5738, now()
);
