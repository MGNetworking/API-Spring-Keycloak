CREATE TABLE users (
    keycloak_id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    user_Name VARCHAR(100) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    birth_date DATE NOT NULL,
    gender VARCHAR(10) ,
    height SMALLINT NOT NULL,
    weight SMALLINT NOT NULL,
    activity_level VARCHAR(20) ,
    goal VARCHAR(20) ,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);


CREATE TABLE user_allergies (
    keycloak_id VARCHAR(36) REFERENCES users(keycloak_id),
    allergy VARCHAR(100),
    PRIMARY KEY (keycloak_id, allergy)
);

CREATE TABLE user_preferences (
    keycloak_id VARCHAR(36) REFERENCES users(keycloak_id),
    preference VARCHAR(100),
    PRIMARY KEY (keycloak_id, preference)
);

CREATE TABLE diets (
    id SERIAL PRIMARY KEY,
    keycloak_id VARCHAR(36) REFERENCES users(keycloak_id),
    name VARCHAR(100),
    type VARCHAR(50),
    calorie_target FLOAT,
    protein_percentage FLOAT,
    carb_percentage FLOAT,
    fat_percentage FLOAT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE meals (
    id SERIAL PRIMARY KEY,
    keycloak_id VARCHAR(36) REFERENCES users(keycloak_id),
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