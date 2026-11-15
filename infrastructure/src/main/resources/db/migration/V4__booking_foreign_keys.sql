-- Claves foráneas de booking hacia publicaciones; van aparte porque esas tablas se crean en la V3.

ALTER TABLE booking ADD CONSTRAINT fk_booking_publication FOREIGN KEY (publication_id) REFERENCES publication (id);
ALTER TABLE booking ADD CONSTRAINT fk_booking_slot FOREIGN KEY (slot_id) REFERENCES publication_slot (id);
ALTER TABLE booking ADD CONSTRAINT fk_booking_proposed_slot FOREIGN KEY (proposed_slot_id) REFERENCES publication_slot (id);
