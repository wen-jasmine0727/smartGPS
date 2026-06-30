-- Backend 1: users / vehicles / cargo / binding / status logs
-- PostgreSQL / TimescaleDB both can run this file.

DROP TABLE IF EXISTS cargo_status_logs;
DROP TABLE IF EXISTS cargo_vehicle_binding;
DROP TABLE IF EXISTS cargo;
DROP TABLE IF EXISTS vehicles;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
  id VARCHAR(32) PRIMARY KEY,
  username VARCHAR(64) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  name VARCHAR(64) NOT NULL,
  role VARCHAR(32) NOT NULL,
  phone VARCHAR(32),
  created_at TIMESTAMPTZ DEFAULT now(),
  updated_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE vehicles (
  id BIGSERIAL PRIMARY KEY,
  plate VARCHAR(32) UNIQUE NOT NULL,
  vin_topic VARCHAR(64) UNIQUE NOT NULL,
  vehicle_type VARCHAR(64),
  capacity INT,
  driver_name VARCHAR(64),
  driver_phone VARCHAR(32),
  device_imei VARCHAR(32) UNIQUE NOT NULL,
  status VARCHAR(32) DEFAULT 'OFFLINE',
  device_status VARCHAR(32) DEFAULT 'OFFLINE',
  created_at TIMESTAMPTZ DEFAULT now(),
  updated_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE cargo (
  cargo_id VARCHAR(64) PRIMARY KEY,
  cargo_type VARCHAR(64),
  weight NUMERIC(10,2),
  status VARCHAR(32) DEFAULT 'CREATED',
  origin_name VARCHAR(128),
  origin_lat DOUBLE PRECISION,
  origin_lng DOUBLE PRECISION,
  destination_name VARCHAR(128),
  destination_lat DOUBLE PRECISION,
  destination_lng DOUBLE PRECISION,
  loaded_at TIMESTAMPTZ,
  delivered_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ DEFAULT now(),
  updated_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE cargo_vehicle_binding (
  id VARCHAR(64) PRIMARY KEY,
  cargo_id VARCHAR(64) NOT NULL REFERENCES cargo(cargo_id),
  plate VARCHAR(32) NOT NULL REFERENCES vehicles(plate),
  status VARCHAR(32) DEFAULT 'ACTIVE',
  bound_at TIMESTAMPTZ DEFAULT now(),
  unbound_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX ux_active_vehicle_binding
  ON cargo_vehicle_binding(plate)
  WHERE status = 'ACTIVE';

CREATE UNIQUE INDEX ux_active_cargo_binding
  ON cargo_vehicle_binding(cargo_id)
  WHERE status = 'ACTIVE';

CREATE TABLE cargo_status_logs (
  id VARCHAR(64) PRIMARY KEY,
  cargo_id VARCHAR(64) NOT NULL REFERENCES cargo(cargo_id),
  status VARCHAR(32) NOT NULL,
  lat DOUBLE PRECISION,
  lng DOUBLE PRECISION,
  remark TEXT,
  operator_id VARCHAR(32),
  created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_cargo_status_logs_cargo_time
  ON cargo_status_logs(cargo_id, created_at DESC);

-- password_hash 为 123456 的 MD5: e10adc3949ba59abbe56e057f20f883e
INSERT INTO users (id, username, password_hash, name, role, phone) VALUES
('USR-001', 'shipper', 'e10adc3949ba59abbe56e057f20f883e', '李货主', 'SHIPPER', '13800000001'),
('USR-002', 'dispatcher', 'e10adc3949ba59abbe56e057f20f883e', '王调度', 'DISPATCHER', '13800000002'),
('USR-003', 'warehouse', 'e10adc3949ba59abbe56e057f20f883e', '赵仓管', 'WAREHOUSE', '13800000003'),
('USR-004', 'admin', 'e10adc3949ba59abbe56e057f20f883e', '系统管理员', 'ADMIN', '13800000004'),
('USR-005', 'driver', 'e10adc3949ba59abbe56e057f20f883e', '张司机', 'DRIVER', '13800000005');

INSERT INTO vehicles (
  plate, vin_topic, vehicle_type, capacity, driver_name, driver_phone,
  device_imei, status, device_status
) VALUES (
  '沪A·C0291', '沪A-C0291', '厢式货车', 5000, '张建国', '13800000001',
  '861234567890123', 'MOVING', 'ONLINE'
);

INSERT INTO cargo (
  cargo_id, cargo_type, weight, status,
  origin_name, origin_lat, origin_lng,
  destination_name, destination_lat, destination_lng,
  loaded_at
) VALUES (
  'SH-HZ-20260629-0291', '电子产品', 2500.00, 'IN_TRANSIT',
  '上海仓储中心', 31.2304, 121.4737,
  '杭州余杭物流中心', 30.2741, 120.1551,
  now()
);

INSERT INTO cargo_vehicle_binding (id, cargo_id, plate, status)
VALUES ('BND-20260629-001', 'SH-HZ-20260629-0291', '沪A·C0291', 'ACTIVE');

INSERT INTO cargo_status_logs (id, cargo_id, status, lat, lng, remark, operator_id)
VALUES ('LOG-20260629-001', 'SH-HZ-20260629-0291', 'LOADED', 31.2304, 121.4737, '上海仓储中心装货完成', 'USR-003');
