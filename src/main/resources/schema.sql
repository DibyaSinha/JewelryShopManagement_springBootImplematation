CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS jewelry (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    company_name VARCHAR(100),
    type ENUM('GOLD', 'SILVER') NOT NULL,
    weight DOUBLE NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    making_percent DOUBLE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS daily_rates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    metal_type ENUM('GOLD', 'SILVER') NOT NULL,
    price_per_gram DOUBLE NOT NULL,
    rate_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_rate_per_day (metal_type, rate_date)
);

CREATE TABLE IF NOT EXISTS customers (
    mobile_number VARCHAR(15) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    discount_percent DOUBLE NOT NULL DEFAULT 0.0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS bills (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    seller_id BIGINT,
    customer_mobile VARCHAR(15),
    total_amount DOUBLE NOT NULL,
    discount_amount DOUBLE NOT NULL DEFAULT 0.0,
    gst_amount DOUBLE NOT NULL,
    grand_total DOUBLE NOT NULL,
    bill_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    pdf_data LONGBLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (seller_id) REFERENCES users(id),
    FOREIGN KEY (customer_mobile) REFERENCES customers(mobile_number) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS bill_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    bill_id BIGINT NOT NULL,
    jewelry_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    rate_at_time DOUBLE NOT NULL,
    base_amount DOUBLE NOT NULL,
    making_charge DOUBLE NOT NULL,
    total_amount DOUBLE NOT NULL,
    FOREIGN KEY (bill_id) REFERENCES bills(id) ON DELETE CASCADE,
    FOREIGN KEY (jewelry_id) REFERENCES jewelry(id)
);

CREATE TABLE IF NOT EXISTS staff (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    mobile_number VARCHAR(15) UNIQUE NOT NULL,
    aadhaar_number VARCHAR(20) UNIQUE NOT NULL,
    gender VARCHAR(10) NOT NULL,
    salary DOUBLE NOT NULL,
    login_access BOOLEAN NOT NULL DEFAULT FALSE,
    username VARCHAR(50) UNIQUE,
    password VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Indexing for performance
CREATE INDEX idx_user_username ON users(username);
CREATE INDEX idx_jewelry_type ON jewelry(type);
CREATE INDEX idx_daily_rates_date ON daily_rates(rate_date);
CREATE INDEX idx_bill_date ON bills(bill_date);
CREATE INDEX idx_staff_mobile ON staff(mobile_number);
