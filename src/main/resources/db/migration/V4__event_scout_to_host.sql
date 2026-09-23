-- event.scout_id -> host_id; each creator becomes the host
-- guarded: the development database was renamed by hand

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'event' AND column_name = 'scout_id'
    ) THEN
        -- column-specific trigger on scout_id; startup recreates it on host_id
        DROP TRIGGER IF EXISTS trg_event_sync_scout ON "event";
        ALTER TABLE "event" RENAME COLUMN scout_id TO host_id;
        ALTER TABLE "event" RENAME CONSTRAINT fk_event_scout_id__id TO fk_event_host_id__id;
        CREATE INDEX IF NOT EXISTS event_host_id ON "event" (host_id);
    END IF;
END $$;
