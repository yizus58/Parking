-- A plate can be inside (status 'IN') only once, across all parkings.
--
-- The application checks this before inserting, but two entries of the same
-- plate into two different parkings lock different parking rows, so both
-- checks could pass concurrently. This partial unique index makes the
-- database reject the second insert; the application maps the violation to
-- 409 "ya está registrado y actualmente en un parking".
--
-- BEFORE DEPLOYING on an existing database, make sure there are no
-- duplicates, or this migration fails (and nothing is changed):
--
--   SELECT upper(trim(plate_number)) AS plate, count(*)
--   FROM "vehicles" WHERE status = 'IN'
--   GROUP BY upper(trim(plate_number)) HAVING count(*) > 1;
--
-- Plates are stored upper-cased and trimmed by the application; this
-- migration normalises the rows that are still inside so the index applies
-- to them consistently.

update "vehicles"
set plate_number = upper(trim(plate_number))
where status = 'IN' and plate_number <> upper(trim(plate_number));

create unique index ux_vehicles_plate_inside
    on "vehicles" (plate_number)
    where status = 'IN';
