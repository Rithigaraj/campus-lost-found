-- Campus Lost & Found Network - MySQL schema
-- Run this script once before starting the application:
--   mysql -u root -p < schema.sql

CREATE DATABASE IF NOT EXISTS campus_lost_found;
USE campus_lost_found;

CREATE TABLE IF NOT EXISTS LOST_ITEMS (
    lost_id      INT AUTO_INCREMENT PRIMARY KEY,
    item_name    VARCHAR(100) NOT NULL,
    category     VARCHAR(50)  NOT NULL,
    description  TEXT,
    location     VARCHAR(150),
    date_lost    DATE         NOT NULL,
    contact_name VARCHAR(100) NOT NULL,
    contact_info VARCHAR(150) NOT NULL,
    status       ENUM('OPEN', 'MATCHED', 'RETURNED', 'CLOSED') NOT NULL DEFAULT 'OPEN'
);

CREATE TABLE IF NOT EXISTS FOUND_ITEMS (
    found_id     INT AUTO_INCREMENT PRIMARY KEY,
    item_name    VARCHAR(100) NOT NULL,
    category     VARCHAR(50)  NOT NULL,
    description  TEXT,
    location     VARCHAR(150),
    date_found   DATE         NOT NULL,
    contact_name VARCHAR(100) NOT NULL,
    contact_info VARCHAR(150) NOT NULL,
    status       ENUM('OPEN', 'MATCHED', 'RETURNED', 'CLOSED') NOT NULL DEFAULT 'OPEN'
);
