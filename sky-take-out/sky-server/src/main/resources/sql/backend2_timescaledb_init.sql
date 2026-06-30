-- Backend 2: TimescaleDB 时序表初始化
-- 请在 Docker TimescaleDB（端口 5433，数据库 gps）中执行此脚本
-- psql -h localhost -p 5433 -U postgres -d gps -f sky-server/src/main/resources/sql/backend2_timescaledb_init.sql

-- 启用 TimescaleDB 扩展（如果尚未启用）
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- 1. GPS 轨迹点超表
DROP TABLE IF EXISTS gps_points CASCADE;

CREATE TABLE gps_points (
  time TIMESTAMPTZ NOT NULL,
  vehicle_id VARCHAR(32) NOT NULL,
  cargo_id VARCHAR(64),
  imei VARCHAR(32),
  lat DOUBLE PRECISION,
  lng DOUBLE PRECISION,
  speed SMALLINT,
  heading SMALLINT,
  accuracy DOUBLE PRECISION
);

-- 转换为超表，按时间自动分区
SELECT create_hypertable('gps_points', 'time', if_not_exists => TRUE);

-- 加速按车辆+时间查询轨迹
CREATE INDEX IF NOT EXISTS idx_gps_vehicle_time ON gps_points(vehicle_id, time DESC);

-- 2. 设备心跳超表
DROP TABLE IF EXISTS device_heartbeats CASCADE;

CREATE TABLE device_heartbeats (
  time TIMESTAMPTZ NOT NULL,
  imei VARCHAR(32) NOT NULL,
  plate VARCHAR(32),
  battery INT,
  signal INT,
  gnss_satellites INT,
  temp DOUBLE PRECISION
);

SELECT create_hypertable('device_heartbeats', 'time', if_not_exists => TRUE);

CREATE INDEX IF NOT EXISTS idx_heartbeat_imei_time ON device_heartbeats(imei, time DESC);

-- 验证
SELECT 'gps_points 超表已创建' AS status;
SELECT 'device_heartbeats 超表已创建' AS status;
