CREATE TABLE dishes (
    id INT PRIMARY KEY,
    name VARCHAR(50),
    price DECIMAL(10, 2),
    category VARCHAR(20)
);

CREATE TABLE orders (
    order_id INT PRIMARY KEY,
    dish_id INT,
    quantity INT,
    order_time DATETIME
);

