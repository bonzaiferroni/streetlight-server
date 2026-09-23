-- city.id: INT -> uuid, carrying every reference through a join on the old id

ALTER TABLE city ADD COLUMN uuid uuid DEFAULT gen_random_uuid() NOT NULL;

ALTER TABLE star ADD COLUMN city_uuid uuid NULL;
ALTER TABLE "location" ADD COLUMN city_uuid uuid NULL;
ALTER TABLE galaxy ADD COLUMN city_uuid uuid NULL;

UPDATE star SET city_uuid = city.uuid FROM city WHERE star.city_id = city.id;
UPDATE "location" SET city_uuid = city.uuid FROM city WHERE "location".city_id = city.id;
UPDATE galaxy SET city_uuid = city.uuid FROM city WHERE galaxy.city_id = city.id;

-- column-specific triggers on location.city_id; startup recreates them
DROP TRIGGER IF EXISTS trg_location_sync_city ON "location";
DROP TRIGGER IF EXISTS trg_location_sync_state ON "location";

ALTER TABLE star DROP CONSTRAINT fk_star_city_id__id;
ALTER TABLE "location" DROP CONSTRAINT fk_location_city_id__id;
ALTER TABLE galaxy DROP CONSTRAINT fk_galaxy_city_id__id;

ALTER TABLE star DROP COLUMN city_id;
ALTER TABLE "location" DROP COLUMN city_id;
ALTER TABLE galaxy DROP COLUMN city_id;

ALTER TABLE city DROP COLUMN id;
DROP SEQUENCE IF EXISTS city_id_seq;

ALTER TABLE city RENAME COLUMN uuid TO id;
ALTER TABLE city ALTER COLUMN id DROP DEFAULT;
ALTER TABLE city ADD PRIMARY KEY (id);

ALTER TABLE star RENAME COLUMN city_uuid TO city_id;
ALTER TABLE "location" RENAME COLUMN city_uuid TO city_id;
ALTER TABLE galaxy RENAME COLUMN city_uuid TO city_id;

ALTER TABLE star ADD CONSTRAINT fk_star_city_id__id FOREIGN KEY (city_id) REFERENCES city(id) ON DELETE SET NULL ON UPDATE RESTRICT;
ALTER TABLE "location" ADD CONSTRAINT fk_location_city_id__id FOREIGN KEY (city_id) REFERENCES city(id) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE galaxy ADD CONSTRAINT fk_galaxy_city_id__id FOREIGN KEY (city_id) REFERENCES city(id) ON DELETE SET NULL ON UPDATE RESTRICT;

CREATE INDEX star_city_id ON star (city_id);
