-- Add numeric primary key for vehicles created from the earlier starter schema.
-- Run this once if your local database was initialized before vehicles.id existed.

CREATE SEQUENCE IF NOT EXISTS vehicles_id_seq;

ALTER TABLE vehicles
  ADD COLUMN IF NOT EXISTS id BIGINT;

ALTER TABLE vehicles
  ALTER COLUMN id SET DEFAULT nextval('vehicles_id_seq');

UPDATE vehicles
SET id = nextval('vehicles_id_seq')
WHERE id IS NULL;

ALTER TABLE vehicles
  ALTER COLUMN id SET NOT NULL;

ALTER SEQUENCE vehicles_id_seq OWNED BY vehicles.id;

SELECT setval(
  'vehicles_id_seq',
  COALESCE((SELECT MAX(id) FROM vehicles), 1),
  EXISTS(SELECT 1 FROM vehicles)
);

ALTER TABLE vehicles
  ALTER COLUMN plate SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_vehicles_plate ON vehicles(plate);

DO $$
DECLARE
  pk_name TEXT;
  pk_columns TEXT;
BEGIN
  SELECT c.conname,
         string_agg(a.attname, ',' ORDER BY a.attnum)
  INTO pk_name, pk_columns
  FROM pg_constraint c
  JOIN unnest(c.conkey) AS cols(attnum) ON TRUE
  JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = cols.attnum
  WHERE c.conrelid = 'vehicles'::regclass
    AND c.contype = 'p'
  GROUP BY c.conname;

  IF pk_columns IS DISTINCT FROM 'id' THEN
    IF to_regclass('cargo_vehicle_binding') IS NOT NULL THEN
      ALTER TABLE cargo_vehicle_binding DROP CONSTRAINT IF EXISTS cargo_vehicle_binding_plate_fkey;
    END IF;

    IF pk_name IS NOT NULL THEN
      EXECUTE format('ALTER TABLE vehicles DROP CONSTRAINT %I', pk_name);
    END IF;

    ALTER TABLE vehicles ADD CONSTRAINT vehicles_pkey PRIMARY KEY (id);

    IF to_regclass('cargo_vehicle_binding') IS NOT NULL THEN
      ALTER TABLE cargo_vehicle_binding
        ADD CONSTRAINT cargo_vehicle_binding_plate_fkey
        FOREIGN KEY (plate) REFERENCES vehicles(plate);
    END IF;
  END IF;
END $$;
