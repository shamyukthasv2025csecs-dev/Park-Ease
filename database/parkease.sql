DROP DATABASE IF EXISTS parkease_db;
CREATE DATABASE parkease_db;
USE parkease_db;

CREATE TABLE vehicles (
    vehicle_id INT PRIMARY KEY AUTO_INCREMENT,
    vehicle_number VARCHAR(20) NOT NULL UNIQUE,
    owner_name VARCHAR(100) NOT NULL,
    vehicle_type VARCHAR(20) NOT NULL
);

CREATE TABLE parking_slots (
    slot_id INT PRIMARY KEY,
    is_occupied TINYINT(1) NOT NULL DEFAULT 0,
    vehicle_id INT NULL,
    CONSTRAINT fk_slot_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id)
);

CREATE TABLE parking_tickets (
    ticket_id INT PRIMARY KEY AUTO_INCREMENT,
    vehicle_id INT NOT NULL,
    ticket_number VARCHAR(30) NOT NULL UNIQUE,
    entry_time DATETIME NOT NULL,
    exit_time DATETIME NULL,
    billable_hours INT DEFAULT 0,
    parking_fee DOUBLE DEFAULT 0,
    ticket_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT fk_ticket_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id)
);

CREATE TABLE ticket_slots (
    ticket_id INT NOT NULL,
    slot_id INT NOT NULL,
    PRIMARY KEY (ticket_id, slot_id),
    CONSTRAINT fk_ticket_slot_ticket FOREIGN KEY (ticket_id) REFERENCES parking_tickets(ticket_id),
    CONSTRAINT fk_ticket_slot_slot FOREIGN KEY (slot_id) REFERENCES parking_slots(slot_id)
);

CREATE TABLE payments (
    payment_id INT PRIMARY KEY AUTO_INCREMENT,
    ticket_id INT NOT NULL UNIQUE,
    amount DOUBLE NOT NULL,
    payment_time DATETIME NOT NULL,
    CONSTRAINT fk_payment_ticket FOREIGN KEY (ticket_id) REFERENCES parking_tickets(ticket_id)
);

INSERT INTO parking_slots (slot_id, is_occupied, vehicle_id) VALUES
(1, 0, NULL), (2, 0, NULL), (3, 0, NULL), (4, 0, NULL), (5, 0, NULL),
(6, 0, NULL), (7, 0, NULL), (8, 0, NULL), (9, 0, NULL), (10, 0, NULL),
(11, 0, NULL), (12, 0, NULL), (13, 0, NULL), (14, 0, NULL), (15, 0, NULL),
(16, 0, NULL), (17, 0, NULL), (18, 0, NULL), (19, 0, NULL), (20, 0, NULL);

SELECT 'ParkEase database initialized successfully.' AS status;
