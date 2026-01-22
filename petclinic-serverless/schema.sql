-- Schema para testes locais com H2

-- Tabela de tipos de pets
CREATE TABLE IF NOT EXISTS types (
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(80)
);

-- Tabela de owners
CREATE TABLE IF NOT EXISTS owners (
  id INT PRIMARY KEY AUTO_INCREMENT,
  first_name VARCHAR(30),
  last_name VARCHAR(30),
  address VARCHAR(255),
  city VARCHAR(80),
  telephone VARCHAR(20)
);

-- Tabela de pets
CREATE TABLE IF NOT EXISTS pets (
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(30),
  birth_date DATE,
  type_id INT,
  owner_id INT,
  FOREIGN KEY (type_id) REFERENCES types(id),
  FOREIGN KEY (owner_id) REFERENCES owners(id)
);

-- Tabela de vets
CREATE TABLE IF NOT EXISTS vets (
  id INT PRIMARY KEY AUTO_INCREMENT,
  first_name VARCHAR(30),
  last_name VARCHAR(30)
);

-- Tabela de especialidades
CREATE TABLE IF NOT EXISTS specialties (
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(80)
);

-- Tabela de relacionamento vet-specialties
CREATE TABLE IF NOT EXISTS vet_specialties (
  vet_id INT,
  specialty_id INT,
  PRIMARY KEY (vet_id, specialty_id),
  FOREIGN KEY (vet_id) REFERENCES vets(id),
  FOREIGN KEY (specialty_id) REFERENCES specialties(id)
);

-- Tabela de visitas
CREATE TABLE IF NOT EXISTS visits (
  id INT PRIMARY KEY AUTO_INCREMENT,
  pet_id INT,
  visit_date DATE,
  description VARCHAR(255),
  FOREIGN KEY (pet_id) REFERENCES pets(id)
);

-- Dados de teste
INSERT INTO types VALUES (1, 'cat'), (2, 'dog'), (3, 'hamster'), (4, 'bird');

INSERT INTO owners VALUES 
  (1, 'John', 'Doe', '123 Main St', 'Springfield', '1234567890'),
  (2, 'Jane', 'Smith', '456 Oak Ave', 'Shelbyville', '9876543210');

INSERT INTO pets VALUES 
  (1, 'Whiskers', '2020-01-15', 1, 1),
  (2, 'Buddy', '2019-05-20', 2, 1),
  (3, 'Fluffy', '2021-03-10', 1, 2);

INSERT INTO specialties VALUES (1, 'radiology'), (2, 'surgery'), (3, 'dentistry');

INSERT INTO vets VALUES 
  (1, 'James', 'Carter'),
  (2, 'Helen', 'Leary'),
  (3, 'Linda', 'Douglas');

INSERT INTO vet_specialties VALUES 
  (1, 1), (1, 2),
  (2, 3),
  (3, 1), (3, 3);

INSERT INTO visits VALUES 
  (1, 1, '2024-01-15', 'Regular checkup'),
  (2, 2, '2024-02-20', 'Vaccination');
