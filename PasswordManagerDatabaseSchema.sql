-- ============================================
-- Password Manager Database Schema
-- ============================================
-- Run this script in Supabase SQL Editor
-- ============================================

-- Drop existing tables if you want a fresh start (OPTIONAL - REMOVES ALL DATA!)
-- Uncomment the lines below if you want to reset everything
-- DROP TABLE IF EXISTS passwords CASCADE;
-- DROP TABLE IF EXISTS users CASCADE;

-- ============================================
-- 1. USERS TABLE
-- ============================================
-- Stores user authentication and TOTP information
CREATE TABLE IF NOT EXISTS users (
    user_id SERIAL PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    master_password_hash TEXT NOT NULL,
    salt TEXT NOT NULL,
    totp_secret TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- 2. PASSWORDS TABLE
-- ============================================
-- Stores encrypted password entries for each user
CREATE TABLE IF NOT EXISTS passwords (
    password_id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    username VARCHAR(255),
    encrypted_password TEXT NOT NULL,
    url VARCHAR(512),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- 3. INDEXES FOR PERFORMANCE
-- ============================================
-- Speed up lookups by username
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);

-- Speed up password queries by user_id
CREATE INDEX IF NOT EXISTS idx_passwords_user_id ON passwords(user_id);

-- Speed up password searches by title
CREATE INDEX IF NOT EXISTS idx_passwords_title ON passwords(title);

-- ============================================
-- 4. TRIGGERS FOR AUTO-UPDATE TIMESTAMPS
-- ============================================
-- Update the updated_at column automatically when a row is modified

-- Create update function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply trigger to users table
DROP TRIGGER IF EXISTS update_users_updated_at ON users;
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Apply trigger to passwords table
DROP TRIGGER IF EXISTS update_passwords_updated_at ON passwords;
CREATE TRIGGER update_passwords_updated_at
    BEFORE UPDATE ON passwords
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- 5. ROW LEVEL SECURITY (OPTIONAL - RECOMMENDED)
-- ============================================
-- Uncomment if you want to enable RLS for extra security
-- This ensures users can only access their own data

-- ALTER TABLE users ENABLE ROW LEVEL SECURITY;
-- ALTER TABLE passwords ENABLE ROW LEVEL SECURITY;

-- CREATE POLICY users_policy ON users
--     FOR ALL
--     USING (true);  -- Adjust this based on your auth requirements

-- CREATE POLICY passwords_policy ON passwords
--     FOR ALL
--     USING (true);  -- Adjust this based on your auth requirements

-- ============================================
-- 6. VERIFICATION QUERIES
-- ============================================
-- Run these to verify tables were created successfully

-- Check if tables exist
SELECT table_name 
FROM information_schema.tables 
WHERE table_schema = 'public' 
ORDER BY table_name;

-- Check users table structure
SELECT column_name, data_type, character_maximum_length
FROM information_schema.columns
WHERE table_name = 'users'
ORDER BY ordinal_position;

-- Check passwords table structure
SELECT column_name, data_type, character_maximum_length
FROM information_schema.columns
WHERE table_name = 'passwords'
ORDER BY ordinal_position;

-- Count existing records (should be 0 if fresh setup)
SELECT 
    (SELECT COUNT(*) FROM users) as user_count,
    (SELECT COUNT(*) FROM passwords) as password_count;
