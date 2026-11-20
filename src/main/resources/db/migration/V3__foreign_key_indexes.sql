-- PostgreSQL does not index foreign keys automatically. These back the
-- frequent lookups: vehicles inside a parking (capacity check on every entry),
-- vehicles registered by a partner, and parkings of an owner.

create index ix_vehicles_parking_status on "vehicles" (id_parking, status);
create index ix_vehicles_admin on "vehicles" (id_admin);
create index ix_parking_lot_owner on "parking_lot" (id_owner);
