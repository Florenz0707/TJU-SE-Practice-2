INSERT INTO "user" (USERNAME, PASSWORD, ACTIVATED, CREATE_TIME, UPDATE_TIME, CREATOR, UPDATER, IS_DELETED)
VALUES ('admin', '$2a$08$lDnHPz7eUkSi6ao14Twuau08mzhWrL4kyZGGU5xfiGALO/Vxd5DOi',TRUE,
        '2025-09-18T12:32:28.9717702', '2025-09-18T12:32:28.9717702', 0, 0, FALSE),
       ('user', '$2a$08$UkVvwpULis18S19S5pZFn.YHPZt3oaqHZnDwqbCW9pft6uFtkXKDC',TRUE,
        '2025-09-18T12:32:28.9717702', '2025-09-18T12:32:28.9717702', 0, 0, FALSE),
       ('business', '$2a$08$UkVvwpULis18S19S5pZFn.YHPZt3oaqHZnDwqbCW9pft6uFtkXKDC',TRUE,
        '2025-09-18T12:32:28.9717702', '2025-09-18T12:32:28.9717702', 0, 0, FALSE)
;

--admin: admin
--user: password

INSERT INTO authority (NAME)
VALUES ('ADMIN'),
       ('USER'),
       ('BUSINESS')
;

INSERT INTO user_authority (USER_ID, AUTHORITY_NAME)
VALUES (1, 'ADMIN'),
       (1, 'BUSINESS'),
       (1, 'USER'),
       (2, 'USER'),
       (3, 'BUSINESS'),
       (3, 'USER')
;
