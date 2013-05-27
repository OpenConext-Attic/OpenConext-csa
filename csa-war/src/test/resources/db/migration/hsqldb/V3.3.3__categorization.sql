INSERT INTO multilingual_string (id) VALUES (1);
INSERT INTO multilingual_string (id) VALUES (2);
INSERT INTO multilingual_string (id) VALUES (3);
INSERT INTO multilingual_string (id) VALUES (4);
INSERT INTO multilingual_string (id) VALUES (5);
INSERT INTO multilingual_string (id) VALUES (6);

INSERT INTO localized_string (id, multilingual_string_id, VALUE, locale) VALUES (1, 1, 'Location', 'en');
INSERT INTO localized_string (id, multilingual_string_id, VALUE, locale) VALUES (2, 2, 'Netherlands', 'en');
INSERT INTO localized_string (id, multilingual_string_id, VALUE, locale) VALUES (3, 3, 'USA', 'en');
INSERT INTO localized_string (id, multilingual_string_id, VALUE, locale) VALUES (4, 4, 'Target Group', 'en');
INSERT INTO localized_string (id, multilingual_string_id, VALUE, locale) VALUES (5, 5, 'Students', 'en');
INSERT INTO localized_string (id, multilingual_string_id, VALUE, locale) VALUES (6, 6, 'Researchers', 'en');

INSERT INTO facet (id, facet_parent_id, multilingual_string_id) VALUES (1, NULL, 1);
INSERT INTO facet (id, facet_parent_id, multilingual_string_id) VALUES (2, NULL, 4);

INSERT INTO facet_value (id, facet_id, multilingual_string_id) VALUES (1, 1, 2);
INSERT INTO facet_value (id, facet_id, multilingual_string_id) VALUES (2, 1, 3);
INSERT INTO facet_value (id, facet_id, multilingual_string_id) VALUES (3, 2, 5);
INSERT INTO facet_value (id, facet_id, multilingual_string_id) VALUES (4, 2, 6);
