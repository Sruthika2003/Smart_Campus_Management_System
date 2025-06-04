CREATE DATABASE smart_campus_db;
USE smart_campus_db;
-- DROP DATABASE smart_campus_db;
-- ===== User Module =====
CREATE TABLE users (
    user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    role ENUM('STUDENT', 'FACULTY', 'ADMIN','ACCOUNTS') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT TRUE
);


-- ===== Course Management Module =====
CREATE TABLE courses (
    course_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_code VARCHAR(20) NOT NULL UNIQUE,
    course_name VARCHAR(100) NOT NULL,
    course_description TEXT,
    credit_hours INT NOT NULL,
    faculty_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (faculty_id) REFERENCES users(user_id)
);

ALTER TABLE courses ADD COLUMN capacity INT DEFAULT 30;
ALTER TABLE courses ADD COLUMN content TEXT;
ALTER TABLE courses ADD COLUMN title VARCHAR(255);

CREATE TABLE enrollments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id BIGINT,
    student_id BIGINT,
    active BOOLEAN,
    enrollment_date DATETIME,
    drop_date DATETIME,
    FOREIGN KEY (course_id) REFERENCES courses(course_id),
    FOREIGN KEY (student_id) REFERENCES users(user_id)
);

select * from enrollments;


CREATE TABLE course_materials (
    material_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    file_name VARCHAR(255),
    file_path VARCHAR(255),
    upload_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    uploaded_by BIGINT NOT NULL,
    FOREIGN KEY (course_id) REFERENCES courses(course_id),
    FOREIGN KEY (uploaded_by) REFERENCES users(user_id)
);

CREATE TABLE material_files (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    material_id BIGINT NOT NULL,
    file_data LONGBLOB NOT NULL,
    FOREIGN KEY (material_id) REFERENCES course_materials(material_id)
);

CREATE TABLE student_courses (
    student_course_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    enrollment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES users(user_id),
    FOREIGN KEY (course_id) REFERENCES courses(course_id),
    UNIQUE KEY (student_id, course_id)
);

CREATE TABLE timetable (
    timetable_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    day_of_week ENUM('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY') NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    room_number VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (course_id) REFERENCES courses(course_id)
);


-- ===== Attendance Module =====
CREATE TABLE attendance (
    attendance_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    attendance_date DATE NOT NULL,
    status ENUM('PRESENT', 'ABSENT', 'LATE', 'EXCUSED') NOT NULL DEFAULT 'ABSENT',
    marked_by BIGINT NOT NULL,
    marked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES users(user_id),
    FOREIGN KEY (course_id) REFERENCES courses(course_id),
    FOREIGN KEY (marked_by) REFERENCES users(user_id),
    UNIQUE KEY (student_id, course_id, attendance_date)
);

CREATE TABLE attendance_reports (
    report_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    month INT NOT NULL,
    year INT NOT NULL,
    total_classes INT NOT NULL DEFAULT 0,
    present_count INT NOT NULL DEFAULT 0,
    absent_count INT NOT NULL DEFAULT 0,
    late_count INT NOT NULL DEFAULT 0,
    excused_count INT NOT NULL DEFAULT 0,
    attendance_percentage DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES users(user_id),
    FOREIGN KEY (course_id) REFERENCES courses(course_id),
    UNIQUE KEY (student_id, course_id, month, year)
);

CREATE TABLE attendance_correction_requests (
    request_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    attendance_id BIGINT NOT NULL,
    requested_by BIGINT NOT NULL,
    reason TEXT NOT NULL,
    status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    reviewed_by BIGINT,
    review_comments TEXT,
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP,
    FOREIGN KEY (attendance_id) REFERENCES attendance(attendance_id),
    FOREIGN KEY (requested_by) REFERENCES users(user_id),
    FOREIGN KEY (reviewed_by) REFERENCES users(user_id)
);

-- ===== Exam and Grading System =====
CREATE TABLE exams (
    exam_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exam_name VARCHAR(100) NOT NULL,
    course_id BIGINT NOT NULL,
    exam_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    exam_type ENUM('MIDTERM', 'FINAL', 'QUIZ', 'ASSIGNMENT') NOT NULL,
    total_marks DECIMAL(5,2) NOT NULL,
    passing_marks DECIMAL(5,2) NOT NULL,
    exam_instructions TEXT,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (course_id) REFERENCES courses(course_id),
    FOREIGN KEY (created_by) REFERENCES users(user_id)
);

CREATE TABLE exam_papers (
    paper_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exam_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(255) NOT NULL,
    upload_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    uploaded_by BIGINT NOT NULL,
    FOREIGN KEY (exam_id) REFERENCES exams(exam_id),
    FOREIGN KEY (uploaded_by) REFERENCES users(user_id)
);

-- Step 1: Create the table
CREATE TABLE grades (
    grade_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    exam_id BIGINT NOT NULL,
    marks_obtained DECIMAL(5,2) NOT NULL,
    percentage DECIMAL(5,2),  -- Now a regular column
    grade_letter VARCHAR(2),
    feedback TEXT,
    graded_by BIGINT NOT NULL,
    graded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES users(user_id),
    FOREIGN KEY (exam_id) REFERENCES exams(exam_id),
    FOREIGN KEY (graded_by) REFERENCES users(user_id),
    UNIQUE KEY (student_id, exam_id)
);

DELIMITER //

CREATE TRIGGER before_insert_grades
BEFORE INSERT ON grades
FOR EACH ROW
BEGIN
    DECLARE total DECIMAL(5,2);
    SELECT total_marks INTO total FROM exams WHERE exam_id = NEW.exam_id;
    SET NEW.percentage = (NEW.marks_obtained * 100.0) / total;
END;
//

CREATE TRIGGER before_update_grades
BEFORE UPDATE ON grades
FOR EACH ROW
BEGIN
    DECLARE total DECIMAL(5,2);
    SELECT total_marks INTO total FROM exams WHERE exam_id = NEW.exam_id;
    SET NEW.percentage = (NEW.marks_obtained * 100.0) / total;
END;


DELIMITER ;

-- ===== Payment and Fee Management =====
CREATE TABLE fee_types (
    fee_type_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    fee_name VARCHAR(100) NOT NULL,
    description TEXT,
    amount DECIMAL(10,2) NOT NULL,
    frequency ENUM('ONE_TIME', 'SEMESTER', 'YEARLY', 'MONTHLY') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE student_fees (
    student_fee_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    fee_type_id BIGINT NOT NULL,
    semester VARCHAR(20),
    academic_year VARCHAR(10),
    amount DECIMAL(10,2) NOT NULL,
    due_date DATE NOT NULL,
    status ENUM('PENDING', 'PAID') NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES users(user_id),
    FOREIGN KEY (fee_type_id) REFERENCES fee_types(fee_type_id)
);

-- Trigger to automatically create fee entries for new students
DELIMITER //

CREATE TRIGGER after_student_insert
AFTER INSERT ON users
FOR EACH ROW
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE fee_id BIGINT;
    DECLARE fee_amount DECIMAL(10,2);
    DECLARE fee_freq ENUM('ONE_TIME', 'SEMESTER', 'YEARLY', 'MONTHLY');
    DECLARE cur CURSOR FOR SELECT fee_type_id, amount, frequency FROM fee_types;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    -- Only create fee entries if the new user is a student
    IF NEW.role = 'STUDENT' THEN
        -- Get current academic year
        SET @current_year = YEAR(CURDATE());
        SET @next_year = @current_year + 1;
        SET @academic_year = CONCAT(@current_year, '-', @next_year);
        
        -- Get current semester based on month
        IF MONTH(CURDATE()) >= 8 THEN
            SET @current_semester = 'Semester 1';
            SET @due_date = CONCAT(@current_year, '-09-30'); -- September 30th
        ELSE
            SET @current_semester = 'Semester 2';
            SET @due_date = CONCAT(@current_year, '-02-28'); -- February 28th
        END IF;
        
        -- Open cursor
        OPEN cur;
        
        -- Start reading fee types
        read_loop: LOOP
            FETCH cur INTO fee_id, fee_amount, fee_freq;
            IF done THEN
                LEAVE read_loop;
            END IF;
            
            -- Insert fee record for the student
            INSERT INTO student_fees (
                student_id,
                fee_type_id,
                semester,
                academic_year,
                amount,
                due_date,
                status
            ) VALUES (
                NEW.user_id,
                fee_id,
                CASE 
                    WHEN fee_freq IN ('SEMESTER', 'MONTHLY') THEN @current_semester
                    ELSE NULL
                END,
                @academic_year,
                fee_amount,
                @due_date,
                'PENDING'
            );
            
        END LOOP;
        
        -- Close cursor
        CLOSE cur;
    END IF;
END //

DELIMITER ;

CREATE TABLE payments (
    payment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    student_fee_id BIGINT NOT NULL,
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    amount DECIMAL(10,2) NOT NULL,
    payment_method ENUM('CASH', 'CREDIT_CARD', 'DEBIT_CARD', 'BANK_TRANSFER', 'ONLINE_PAYMENT') NOT NULL,
    transaction_reference VARCHAR(100),
    receipt_number VARCHAR(50) NOT NULL UNIQUE,
    recorded_by BIGINT NOT NULL,
    remarks TEXT,
    FOREIGN KEY (student_id) REFERENCES users(user_id),
    FOREIGN KEY (student_fee_id) REFERENCES student_fees(student_fee_id),
    FOREIGN KEY (recorded_by) REFERENCES users(user_id)
);

-- Faculty Salary Management
CREATE TABLE faculty_salaries (
    salary_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    faculty_id BIGINT NOT NULL,
    salary_date DATE NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    payment_method ENUM('BANK_TRANSFER', 'CHEQUE', 'CASH') NOT NULL,
    transaction_reference VARCHAR(100),
    status ENUM('PENDING', 'PAID', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    credited_by BIGINT NOT NULL,
    credited_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remarks TEXT,
    FOREIGN KEY (faculty_id) REFERENCES users(user_id),
    FOREIGN KEY (credited_by) REFERENCES users(user_id)
);

CREATE INDEX idx_faculty_salaries_faculty ON faculty_salaries(faculty_id);
CREATE INDEX idx_faculty_salaries_date ON faculty_salaries(salary_date);
CREATE INDEX idx_faculty_salaries_status ON faculty_salaries(status);

-- Insert sample data for testing purposes
-- Insert sample users
INSERT INTO users (username, password, email, first_name, last_name, role)
VALUES 
('admin', 'pass123', 'admin@example.com', 'Alice', 'Admin', 'ADMIN'),
('faculty1', 'pass123', 'faculty@example.com', 'Bob', 'Brown', 'FACULTY'),
('student1', 'pass123', 'student1@example.com', 'Charlie', 'Clark', 'STUDENT'),
('student2', 'pass123', 'student2@example.com', 'Dana', 'Doe', 'STUDENT'),
('accounts1', 'pass123', 'accounts1@example.com', 'Emma', 'Evans', 'ACCOUNTS');

INSERT INTO users (username, password, email, first_name, last_name, role)
VALUES
  ('admin2', 'pass123', 'admin2@example.com', 'Alex', 'Anderson', 'ADMIN');

INSERT INTO users (username, password, email, first_name, last_name, role)
VALUES
  ('student3', 'pass123', 'student3@example.com', 'neha', 'D', 'STUDENT');

INSERT INTO courses (course_code, course_name, course_description, credit_hours, faculty_id)
VALUES 
('CS101', 'Intro to CS', 'Basics of Computer Science', 3, 2);

INSERT INTO timetable (course_id, day_of_week, start_time, end_time, room_number)
VALUES 
(1, 'MONDAY', '10:00:00', '11:00:00', 'A101'),
(1, 'WEDNESDAY', '10:00:00', '11:00:00', 'A101');

INSERT INTO attendance (student_id, course_id, attendance_date, status, marked_by)
VALUES 
(3, 1, '2023-09-04', 'PRESENT', 2),
(3, 1, '2023-09-06', 'ABSENT', 2),
(4, 1, '2023-09-04', 'LATE', 2),
(4, 1, '2023-09-06', 'PRESENT', 2);

INSERT INTO exams (exam_name, course_id, exam_date, start_time, end_time, exam_type, total_marks, passing_marks, created_by)
VALUES 
('Midterm Exam', 1, '2023-10-01', '09:00:00', '11:00:00', 'MIDTERM', 100.00, 40.00, 2);

INSERT INTO grades (student_id, exam_id, marks_obtained, grade_letter, feedback, graded_by)
VALUES 
(3, 1, 85.00, 'A', 'Great job', 2),
(4, 1, 70.00, 'B', 'Good effort', 2);

-- Insert Tuition Fee (id = 1)
INSERT INTO fee_types (fee_name, description, amount, frequency)
VALUES ('Tuition Fee', 'Semester-wise tuition fee', 25000.00, 'SEMESTER');

-- Insert Library Fee (id = 2)
INSERT INTO fee_types (fee_name, description, amount, frequency)
VALUES ('Library Fee', 'Annual library membership', 2000.00, 'YEARLY');

INSERT INTO fee_types (fee_name, description, amount, frequency)
VALUES ('Bus Fee', 'Semester-wise bus fee', 30000.00, 'SEMESTER');

INSERT INTO fee_types (fee_name, description, amount, frequency)
VALUES ('Other Fee', 'Semester-wise other fee', 10000.00, 'SEMESTER');

INSERT INTO fee_types (fee_name, description, amount, frequency)
VALUES ('Exam Fee', 'Semester-wise tuition fee', 2500.00, 'SEMESTER');

INSERT INTO fee_types (fee_name, description, amount, frequency)
VALUES ('Admission Fee', 'One time Admission fee', 125000.00, 'ONE_TIME');

INSERT INTO student_fees (student_id, fee_type_id, semester, academic_year, amount, due_date)
VALUES 
(3, 1, 'Semester 1', '2023-24', 25000.00, '2023-10-01'),
(3, 2, 'Semester 1', '2023-24', 2000.00, '2023-10-10'),
(4, 1, 'Semester 1', '2023-24', 25000.00, '2023-10-01');




-- Create indexes for better performance
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_courses_faculty ON courses(faculty_id);
CREATE INDEX idx_student_courses_student ON student_courses(student_id);
CREATE INDEX idx_student_courses_course ON student_courses(course_id);
CREATE INDEX idx_attendance_student ON attendance(student_id);
CREATE INDEX idx_attendance_course ON attendance(course_id);
CREATE INDEX idx_attendance_date ON attendance(attendance_date);
CREATE INDEX idx_exams_course ON exams(course_id);
CREATE INDEX idx_exams_date ON exams(exam_date);
CREATE INDEX idx_grades_student ON grades(student_id);
CREATE INDEX idx_grades_exam ON grades(exam_id);
CREATE INDEX idx_student_fees_student ON student_fees(student_id);
CREATE INDEX idx_student_fees_status ON student_fees(status);
CREATE INDEX idx_payments_student ON payments(student_id);

-- INSERT INTO courses (course_code, course_name, course_description, credit_hours, faculty_id)
-- VALUES ('CS101', 'Data Structures', 'Intro to data structures', 3, NULL);



INSERT INTO student_courses (student_id, course_id)
VALUES (3, 1);
INSERT INTO users (username, password, email, first_name, last_name, role)
VALUES
  ('student4', 'pass123', 'student4@example.com', 'Alex', 'Anderson', 'STUDENT');

INSERT INTO users (username, password, email, first_name, last_name, role)
VALUES
  ('accounts1', 'pass123', 'accounts1@example.com', 'Alex', 'Anderson', 'ACCOUNTS');

CREATE TABLE fee_alerts (
    alert_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_fee_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    accounts_id BIGINT NOT NULL,
    alert_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    alert_type ENUM('EMAIL', 'SMS', 'SYSTEM') NOT NULL DEFAULT 'SYSTEM',
    status ENUM('SENT', 'PENDING', 'FAILED') NOT NULL DEFAULT 'SENT',
    message LONGTEXT,
    FOREIGN KEY (student_fee_id) REFERENCES student_fees(student_fee_id),
    FOREIGN KEY (student_id) REFERENCES users(user_id),
    FOREIGN KEY (accounts_id) REFERENCES users(user_id)
);

CREATE INDEX idx_fee_alerts_student ON fee_alerts(student_id);
CREATE INDEX idx_fee_alerts_fee ON fee_alerts(student_fee_id);
CREATE INDEX idx_fee_alerts_accounts ON fee_alerts(accounts_id);
CREATE INDEX idx_fee_alerts_date ON fee_alerts(alert_date);
select * from student_fees;

CREATE TABLE revaluation_requests (
    request_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    exam_id BIGINT NOT NULL,
    grade_id BIGINT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    requested_at DATETIME NOT NULL,
    processed_at DATETIME,
    processed_by BIGINT,
    processing_notes TEXT,
    FOREIGN KEY (student_id) REFERENCES users(user_id),
    FOREIGN KEY (exam_id) REFERENCES exams(exam_id),
    FOREIGN KEY (grade_id) REFERENCES grades(grade_id),
    FOREIGN KEY (processed_by) REFERENCES users(user_id)
);

CREATE INDEX idx_revaluation_requests_grade ON revaluation_requests(grade_id);
CREATE INDEX idx_revaluation_requests_student ON revaluation_requests(student_id);
CREATE INDEX idx_revaluation_requests_status ON revaluation_requests(status);
