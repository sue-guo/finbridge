-- V1: Category hierarchy and classification rules

CREATE TABLE categories (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(60)  NOT NULL UNIQUE,
    display_name VARCHAR(60),
    parent_id    BIGINT REFERENCES categories(id),
    expense_type VARCHAR(20)  NOT NULL DEFAULT 'FLEXIBLE'
);

-- Top-level categories
INSERT INTO categories (name, display_name, expense_type) VALUES
    ('FOOD',          'Food & Dining',       'FLEXIBLE'),
    ('TRANSPORT',     'Transportation',       'FLEXIBLE'),
    ('SHOPPING',      'Shopping',             'FLEXIBLE'),
    ('HOUSING',       'Housing',              'RECURRING'),
    ('UTILITIES',     'Utilities',            'RECURRING'),
    ('SUBSCRIPTIONS', 'Subscriptions',        'RECURRING'),
    ('HEALTH',        'Health & Medical',     'FLEXIBLE'),
    ('ENTERTAINMENT', 'Entertainment',        'FLEXIBLE'),
    ('INCOME',        'Income',               'FLEXIBLE'),
    ('OTHER',         'Other',                'FLEXIBLE');

-- Sub-categories (children)
INSERT INTO categories (name, display_name, parent_id, expense_type) VALUES
    ('DINING_OUT',    'Dining Out',     (SELECT id FROM categories WHERE name='FOOD'),          'FLEXIBLE'),
    ('GROCERIES',     'Groceries',      (SELECT id FROM categories WHERE name='FOOD'),          'FLEXIBLE'),
    ('COFFEE',        'Coffee',         (SELECT id FROM categories WHERE name='FOOD'),          'FLEXIBLE'),
    ('GAS',           'Gas & Fuel',     (SELECT id FROM categories WHERE name='TRANSPORT'),     'FLEXIBLE'),
    ('TRANSIT',       'Public Transit', (SELECT id FROM categories WHERE name='TRANSPORT'),     'FLEXIBLE'),
    ('RIDESHARE',     'Rideshare',      (SELECT id FROM categories WHERE name='TRANSPORT'),     'FLEXIBLE'),
    ('RENT',          'Rent',           (SELECT id FROM categories WHERE name='HOUSING'),       'RECURRING'),
    ('INSURANCE',     'Insurance',      (SELECT id FROM categories WHERE name='HOUSING'),       'RECURRING'),
    ('INTERNET',      'Internet',       (SELECT id FROM categories WHERE name='UTILITIES'),     'RECURRING'),
    ('PHONE',         'Phone',          (SELECT id FROM categories WHERE name='UTILITIES'),     'RECURRING'),
    ('ELECTRICITY',   'Electricity',    (SELECT id FROM categories WHERE name='UTILITIES'),     'RECURRING'),
    ('STREAMING',     'Streaming',      (SELECT id FROM categories WHERE name='SUBSCRIPTIONS'), 'RECURRING'),
    ('GYM',           'Gym',            (SELECT id FROM categories WHERE name='SUBSCRIPTIONS'), 'RECURRING'),
    ('PHARMACY',      'Pharmacy',       (SELECT id FROM categories WHERE name='HEALTH'),        'FLEXIBLE'),
    ('SALARY',        'Salary',         (SELECT id FROM categories WHERE name='INCOME'),        'FLEXIBLE');

CREATE TABLE category_rules (
    id          BIGSERIAL PRIMARY KEY,
    keyword     VARCHAR(120) NOT NULL,
    category_id BIGINT NOT NULL REFERENCES categories(id),
    priority    INT NOT NULL DEFAULT 100
);

CREATE INDEX idx_rule_keyword ON category_rules(keyword);

-- Canadian merchant keyword rules (priority: lower = higher precedence)
INSERT INTO category_rules (keyword, category_id, priority) VALUES
    -- Coffee
    ('TIM HORTON',    (SELECT id FROM categories WHERE name='COFFEE'),     10),
    ('STARBUCKS',     (SELECT id FROM categories WHERE name='COFFEE'),     10),
    ('SECOND CUP',    (SELECT id FROM categories WHERE name='COFFEE'),     10),
    ('TIMOTHY',       (SELECT id FROM categories WHERE name='COFFEE'),     10),
    -- Dining
    ('MCDONALD',      (SELECT id FROM categories WHERE name='DINING_OUT'), 20),
    ('SUBWAY',        (SELECT id FROM categories WHERE name='DINING_OUT'), 20),
    ('PIZZA',         (SELECT id FROM categories WHERE name='DINING_OUT'), 20),
    ('RESTAURANT',    (SELECT id FROM categories WHERE name='DINING_OUT'), 20),
    ('DOORDASH',      (SELECT id FROM categories WHERE name='DINING_OUT'), 20),
    ('UBER EATS',     (SELECT id FROM categories WHERE name='DINING_OUT'), 20),
    ('SKIP THE',      (SELECT id FROM categories WHERE name='DINING_OUT'), 20),
    -- Groceries
    ('LOBLAWS',       (SELECT id FROM categories WHERE name='GROCERIES'),  20),
    ('SOBEYS',        (SELECT id FROM categories WHERE name='GROCERIES'),  20),
    ('METRO',         (SELECT id FROM categories WHERE name='GROCERIES'),  20),
    ('NO FRILLS',     (SELECT id FROM categories WHERE name='GROCERIES'),  20),
    ('FOOD BASIC',    (SELECT id FROM categories WHERE name='GROCERIES'),  20),
    ('WALMART',       (SELECT id FROM categories WHERE name='GROCERIES'),  20),
    ('COSTCO',        (SELECT id FROM categories WHERE name='GROCERIES'),  20),
    -- Transit
    ('PRESTO',        (SELECT id FROM categories WHERE name='TRANSIT'),    15),
    ('OC TRANSPO',    (SELECT id FROM categories WHERE name='TRANSIT'),    15),
    ('TTC',           (SELECT id FROM categories WHERE name='TRANSIT'),    15),
    -- Rideshare
    ('UBER',          (SELECT id FROM categories WHERE name='RIDESHARE'),  25),
    ('LYFT',          (SELECT id FROM categories WHERE name='RIDESHARE'),  25),
    -- Gas
    ('PETRO',         (SELECT id FROM categories WHERE name='GAS'),        20),
    ('ESSO',          (SELECT id FROM categories WHERE name='GAS'),        20),
    ('SHELL',         (SELECT id FROM categories WHERE name='GAS'),        20),
    ('CANADIAN TIRE', (SELECT id FROM categories WHERE name='GAS'),        20),
    -- Streaming
    ('NETFLIX',       (SELECT id FROM categories WHERE name='STREAMING'),  10),
    ('SPOTIFY',       (SELECT id FROM categories WHERE name='STREAMING'),  10),
    ('DISNEY',        (SELECT id FROM categories WHERE name='STREAMING'),  10),
    ('AMAZON PRIME',  (SELECT id FROM categories WHERE name='STREAMING'),  10),
    ('YOUTUBE',       (SELECT id FROM categories WHERE name='STREAMING'),  10),
    -- Utilities
    ('HYDRO',         (SELECT id FROM categories WHERE name='ELECTRICITY'),15),
    ('ENBRIDGE',      (SELECT id FROM categories WHERE name='UTILITIES'),  15),
    ('ROGERS',        (SELECT id FROM categories WHERE name='PHONE'),      15),
    ('BELL',          (SELECT id FROM categories WHERE name='PHONE'),      15),
    ('TELUS',         (SELECT id FROM categories WHERE name='PHONE'),      15),
    ('FIDO',          (SELECT id FROM categories WHERE name='PHONE'),      15),
    -- Gym
    ('GOODLIFE',      (SELECT id FROM categories WHERE name='GYM'),        10),
    ('PLANET FITNESS',(SELECT id FROM categories WHERE name='GYM'),        10),
    ('YMCA',          (SELECT id FROM categories WHERE name='GYM'),        10),
    -- Income
    ('PAYROLL',       (SELECT id FROM categories WHERE name='SALARY'),     5),
    ('DIRECT DEP',    (SELECT id FROM categories WHERE name='SALARY'),     5),
    ('SALARY',        (SELECT id FROM categories WHERE name='SALARY'),     5);
