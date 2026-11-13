CREATE TABLE material (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id TEXT NOT NULL,
    article_number TEXT NOT NULL,
    name TEXT NOT NULL,
    manufacturer TEXT NOT NULL,
    description TEXT NOT NULL,
    category TEXT NOT NULL,
    unit TEXT NOT NULL,
    price NUMERIC NOT NULL CHECK (price >= 0),
    currency VARCHAR(3) NOT NULL,
    UNIQUE (owner_id, article_number)
);
