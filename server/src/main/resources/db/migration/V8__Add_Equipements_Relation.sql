-- V8: Add Predefined Equipements and many-to-many relationship with Logements

CREATE TABLE equipements (
    id UUID PRIMARY KEY,
    key VARCHAR(50) NOT NULL UNIQUE,
    label VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE logement_equipements (
    logement_id UUID NOT NULL REFERENCES logements(id) ON DELETE CASCADE,
    equipement_id UUID NOT NULL REFERENCES equipements(id) ON DELETE CASCADE,
    PRIMARY KEY (logement_id, equipement_id)
);

-- Seed predefined standard equipments (Generate unique robust UUIDs)
INSERT INTO equipements (id, key, label, created_at, updated_at) VALUES
('a8385da4-984a-4a25-8321-ee8a58a47111', 'WIFI', 'Wi-Fi Fibre', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b1928374-27ac-4ba5-a831-92ee5a711222', 'CLIM', 'Climatisation', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c2839485-3bc4-411a-b328-9eed85bc7333', 'EAU', 'Eau Courante', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('d384950a-ee8a-4d2c-8344-9f2ea7da8444', 'PARKING', 'Parking Sécurisé', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('e92847ea-ff8b-4a55-8311-9fa2a8f82555', 'GENERATEUR', 'Groupe Électrogène', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
