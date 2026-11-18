CREATE DATABASE IF NOT EXISTS campus_events;
USE campus_events;

CREATE TABLE IF NOT EXISTS event_registrations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    reg_no VARCHAR(20) NOT NULL,
    student_name VARCHAR(100) NOT NULL,
    email VARCHAR(120) NOT NULL,
    event_name VARCHAR(80) NOT NULL,
    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_student_event UNIQUE (reg_no, event_name)
);

INSERT IGNORE INTO event_registrations (reg_no, student_name, email, event_name) VALUES
('23CS101', 'Aarav Sharma', 'aarav@college.edu', 'Hackathon 2026'),
('23CS102', 'Diya Nair', 'diya@college.edu', 'Robotics Expo'),
('23EC105', 'Rohan Verma', 'rohan@college.edu', 'Hackathon 2026');
