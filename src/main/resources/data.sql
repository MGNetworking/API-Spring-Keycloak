CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    keycloak_id VARCHAR(36) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    birth_date DATE,
    gender VARCHAR(10),
    height FLOAT,
    weight FLOAT,
    activity_level VARCHAR(20),
    goal VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE user_allergies (
    user_id INTEGER REFERENCES users(id),
    allergy VARCHAR(100),
    PRIMARY KEY (user_id, allergy)
);

CREATE TABLE user_preferences (
    user_id INTEGER REFERENCES users(id),
    preference VARCHAR(100),
    PRIMARY KEY (user_id, preference)
);

CREATE TABLE diets (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(id),
    name VARCHAR(100),
    type VARCHAR(50),
    calorie_target FLOAT,
    protein_percentage FLOAT,
    carb_percentage FLOAT,
    fat_percentage FLOAT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE meals (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(id),
    name VARCHAR(100),
    meal_type VARCHAR(20),
    date_time TIMESTAMP,
    notes TEXT
);

CREATE TABLE meal_items (
    id SERIAL PRIMARY KEY,
    meal_id INTEGER REFERENCES meals(id),
    food_external_id VARCHAR(100),
    food_name VARCHAR(255),
    quantity FLOAT,
    calories FLOAT,
    proteins FLOAT,
    carbs FLOAT,
    fats FLOAT
);