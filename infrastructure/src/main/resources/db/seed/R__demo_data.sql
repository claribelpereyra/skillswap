-- Reproducible demo seed data (Flyway repeatable migration, safe against subsequent V* migrations).

INSERT INTO category (id, parent_id, name, kind)
SELECT 1, NULL, 'Tecnología', 'CATEGORY'
FROM (SELECT 1 AS seed) AS s
WHERE NOT EXISTS (SELECT 1 FROM category WHERE id = 1);

INSERT INTO category (id, parent_id, name, kind)
SELECT 2, 1, 'Java', 'SKILL'
FROM (SELECT 1 AS seed) AS s
WHERE NOT EXISTS (SELECT 1 FROM category WHERE id = 2);

INSERT INTO category (id, parent_id, name, kind)
SELECT 3, 1, 'Clean Architecture', 'SKILL'
FROM (SELECT 1 AS seed) AS s
WHERE NOT EXISTS (SELECT 1 FROM category WHERE id = 3);

INSERT INTO category (id, parent_id, name, kind)
SELECT 4, 1, 'TypeScript', 'SKILL'
FROM (SELECT 1 AS seed) AS s
WHERE NOT EXISTS (SELECT 1 FROM category WHERE id = 4);

INSERT INTO category (id, parent_id, name, kind)
SELECT 5, 1, 'Testing', 'SKILL'
FROM (SELECT 1 AS seed) AS s
WHERE NOT EXISTS (SELECT 1 FROM category WHERE id = 5);

INSERT INTO publication (id, author_id, type, status, title, description, category_id, modality, duration_minutes, capacity, base_price, created_at, published_at, version)
SELECT 1, 'ana', 'MENTORING', 'PUBLISHED', 'Hexagonal Architecture Coaching', 'Hands-on session on Clean Architecture and testing in Java', 3, 'REMOTE', 60, 2, 100, '2026-11-01 12:15:00', '2026-11-01 12:20:00', 0
FROM (SELECT 1 AS seed) AS s
WHERE NOT EXISTS (SELECT 1 FROM publication WHERE id = 1);

INSERT INTO publication (id, author_id, type, status, title, description, category_id, modality, duration_minutes, capacity, base_price, created_at, published_at, version)
SELECT 2, 'techlab', 'CHALLENGE', 'PUBLISHED', 'Open Source Refactoring Sprint', 'Collaborative weekend challenge on clean code practices', 5, 'REMOTE', 120, 10, 0, '2026-11-02 10:00:00', '2026-11-02 10:15:00', 0
FROM (SELECT 1 AS seed) AS s
WHERE NOT EXISTS (SELECT 1 FROM publication WHERE id = 2);

INSERT INTO publication_slot (id, publication_id, starts_at, ends_at, occupied)
SELECT 1, 1, '2026-11-05 17:00:00', '2026-11-05 18:00:00', TRUE
FROM (SELECT 1 AS seed) AS s
WHERE NOT EXISTS (SELECT 1 FROM publication_slot WHERE id = 1);

INSERT INTO publication_slot (id, publication_id, starts_at, ends_at, occupied)
SELECT 2, 1, '2026-11-18 17:00:00', '2026-11-18 18:00:00', FALSE
FROM (SELECT 1 AS seed) AS s
WHERE NOT EXISTS (SELECT 1 FROM publication_slot WHERE id = 2);

INSERT INTO publication_transition (publication_id, position, from_status, to_status, actor_id, occurred_at, reason)
SELECT 1, 0, NULL, 'DRAFT', 'ana', '2026-11-01 12:15:00', NULL
FROM (SELECT 1 AS seed) AS s
WHERE NOT EXISTS (SELECT 1 FROM publication_transition WHERE publication_id = 1 AND position = 0);

INSERT INTO publication_transition (publication_id, position, from_status, to_status, actor_id, occurred_at, reason)
SELECT 1, 1, 'DRAFT', 'PUBLISHED', 'ana', '2026-11-01 12:20:00', NULL
FROM (SELECT 1 AS seed) AS s
WHERE NOT EXISTS (SELECT 1 FROM publication_transition WHERE publication_id = 1 AND position = 1);

INSERT INTO publication_transition (publication_id, position, from_status, to_status, actor_id, occurred_at, reason)
SELECT 2, 0, NULL, 'DRAFT', 'techlab', '2026-11-02 10:00:00', NULL
FROM (SELECT 1 AS seed) AS s
WHERE NOT EXISTS (SELECT 1 FROM publication_transition WHERE publication_id = 2 AND position = 0);

INSERT INTO publication_transition (publication_id, position, from_status, to_status, actor_id, occurred_at, reason)
SELECT 2, 1, 'DRAFT', 'PUBLISHED', 'techlab', '2026-11-02 10:15:00', NULL
FROM (SELECT 1 AS seed) AS s
WHERE NOT EXISTS (SELECT 1 FROM publication_transition WHERE publication_id = 2 AND position = 1);

INSERT INTO booking (id, publication_id, client_id, provider_id, slot_id, slot_starts_at, slot_ends_at, message, goal, price, status, version)
SELECT 1, 1, 'bruno', 'ana', 1, '2026-11-05 17:00:00', '2026-11-05 18:00:00', 'Hola Ana, me gustaría revisar arquitectura hexagonal.', 'Practicar diseño modular en Java', 100, 'COMPLETED', 0
FROM (SELECT 1 AS seed) AS s
WHERE NOT EXISTS (SELECT 1 FROM booking WHERE id = 1);

INSERT INTO booking_transition (booking_id, position, from_status, to_status, actor_id, occurred_at, reason)
SELECT 1, 0, NULL, 'PENDING', 'bruno', '2026-11-03 10:00:00', NULL
FROM (SELECT 1 AS seed) AS s
WHERE NOT EXISTS (SELECT 1 FROM booking_transition WHERE booking_id = 1 AND position = 0);

INSERT INTO booking_transition (booking_id, position, from_status, to_status, actor_id, occurred_at, reason)
SELECT 1, 1, 'PENDING', 'ACCEPTED', 'ana', '2026-11-03 11:00:00', NULL
FROM (SELECT 1 AS seed) AS s
WHERE NOT EXISTS (SELECT 1 FROM booking_transition WHERE booking_id = 1 AND position = 1);

INSERT INTO booking_transition (booking_id, position, from_status, to_status, actor_id, occurred_at, reason)
SELECT 1, 2, 'ACCEPTED', 'CONFIRMED', 'bruno', '2026-11-03 12:00:00', NULL
FROM (SELECT 1 AS seed) AS s
WHERE NOT EXISTS (SELECT 1 FROM booking_transition WHERE booking_id = 1 AND position = 2);

INSERT INTO booking_transition (booking_id, position, from_status, to_status, actor_id, occurred_at, reason)
SELECT 1, 3, 'CONFIRMED', 'IN_PROGRESS', 'ana', '2026-11-05 17:00:00', NULL
FROM (SELECT 1 AS seed) AS s
WHERE NOT EXISTS (SELECT 1 FROM booking_transition WHERE booking_id = 1 AND position = 3);

INSERT INTO booking_transition (booking_id, position, from_status, to_status, actor_id, occurred_at, reason)
SELECT 1, 4, 'IN_PROGRESS', 'COMPLETED', 'bruno', '2026-11-05 18:00:00', NULL
FROM (SELECT 1 AS seed) AS s
WHERE NOT EXISTS (SELECT 1 FROM booking_transition WHERE booking_id = 1 AND position = 4);

INSERT INTO profile (user_id, display_name, bio, time_zone, account_type, reputation, rating_sum, rating_count, availability, active, created_at, version)
SELECT 'ana', 'Ana Perez', 'Java and Clean Architecture mentor', 'America/Argentina/Buenos_Aires', 'PROFESSIONAL', 5.0, 5, 1, 'Weekdays 18:00-21:00', TRUE, '2026-11-01 12:00:00', 0
FROM (SELECT 1 AS seed) AS s
WHERE NOT EXISTS (SELECT 1 FROM profile WHERE user_id = 'ana');

INSERT INTO profile (user_id, display_name, bio, time_zone, account_type, reputation, rating_sum, rating_count, availability, active, created_at, version)
SELECT 'bruno', 'Bruno Diaz', 'Full-stack TypeScript engineer', 'America/Argentina/Buenos_Aires', 'PROFESSIONAL', 4.0, 4, 1, 'Saturdays 10:00-14:00', TRUE, '2026-11-01 12:05:00', 0
FROM (SELECT 1 AS seed) AS s
WHERE NOT EXISTS (SELECT 1 FROM profile WHERE user_id = 'bruno');

INSERT INTO profile (user_id, display_name, bio, time_zone, account_type, reputation, rating_sum, rating_count, availability, active, created_at, version)
SELECT 'techlab', 'TechLab UADE', 'Applied software engineering workshops', 'America/Argentina/Buenos_Aires', 'ORGANIZATION', 0.0, 0, 0, 'Mondays and Wednesdays 19:00-21:00', TRUE, '2026-11-01 12:10:00', 0
FROM (SELECT 1 AS seed) AS s
WHERE NOT EXISTS (SELECT 1 FROM profile WHERE user_id = 'techlab');

INSERT INTO profile_skill (profile_id, skill_id)
SELECT p.id, 2
FROM profile p
WHERE p.user_id = 'ana'
  AND NOT EXISTS (SELECT 1 FROM profile_skill ps WHERE ps.profile_id = p.id AND ps.skill_id = 2);

INSERT INTO profile_skill (profile_id, skill_id)
SELECT p.id, 3
FROM profile p
WHERE p.user_id = 'ana'
  AND NOT EXISTS (SELECT 1 FROM profile_skill ps WHERE ps.profile_id = p.id AND ps.skill_id = 3);

INSERT INTO profile_skill (profile_id, skill_id)
SELECT p.id, 4
FROM profile p
WHERE p.user_id = 'bruno'
  AND NOT EXISTS (SELECT 1 FROM profile_skill ps WHERE ps.profile_id = p.id AND ps.skill_id = 4);

INSERT INTO profile_skill (profile_id, skill_id)
SELECT p.id, 5
FROM profile p
WHERE p.user_id = 'techlab'
  AND NOT EXISTS (SELECT 1 FROM profile_skill ps WHERE ps.profile_id = p.id AND ps.skill_id = 5);

INSERT INTO profile_interest (profile_id, interest)
SELECT p.id, 'UX Design'
FROM profile p
WHERE p.user_id = 'ana'
  AND NOT EXISTS (SELECT 1 FROM profile_interest pi WHERE pi.profile_id = p.id AND pi.interest = 'UX Design');

INSERT INTO profile_interest (profile_id, interest)
SELECT p.id, 'Domain-Driven Design'
FROM profile p
WHERE p.user_id = 'bruno'
  AND NOT EXISTS (SELECT 1 FROM profile_interest pi WHERE pi.profile_id = p.id AND pi.interest = 'Domain-Driven Design');

INSERT INTO rating (booking_id, author_id, recipient_id, score, comment, rated_at)
SELECT 1, 'bruno', 'ana', 5, 'Very clear explanation of Hexagonal Architecture', '2026-11-05 18:30:00'
FROM (SELECT 1 AS seed) AS s
WHERE NOT EXISTS (SELECT 1 FROM rating WHERE booking_id = 1 AND author_id = 'bruno');

INSERT INTO rating (booking_id, author_id, recipient_id, score, comment, rated_at)
SELECT 1, 'ana', 'bruno', 4, 'Great preparation and punctual session', '2026-11-05 18:45:00'
FROM (SELECT 1 AS seed) AS s
WHERE NOT EXISTS (SELECT 1 FROM rating WHERE booking_id = 1 AND author_id = 'ana');
