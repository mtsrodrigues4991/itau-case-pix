-- Seed das contas conhecidas pelo banco. O payload do case nao traz documento do titular,
-- entao o tipo de pessoa (e o limite de 5 ou 20 chaves) vem daqui.
-- Os INSERTs sao idempotentes porque o perfil dev usa ddl-auto=update e a base sobrevive ao restart.

INSERT INTO account (numero_agencia, numero_conta, tipo_pessoa)
SELECT '1234', '12345678', 'PF'
WHERE NOT EXISTS (SELECT 1 FROM account WHERE numero_agencia = '1234' AND numero_conta = '12345678');

INSERT INTO account (numero_agencia, numero_conta, tipo_pessoa)
SELECT '1234', '87654321', 'PF'
WHERE NOT EXISTS (SELECT 1 FROM account WHERE numero_agencia = '1234' AND numero_conta = '87654321');

INSERT INTO account (numero_agencia, numero_conta, tipo_pessoa)
SELECT '4321', '11223344', 'PJ'
WHERE NOT EXISTS (SELECT 1 FROM account WHERE numero_agencia = '4321' AND numero_conta = '11223344');

INSERT INTO account (numero_agencia, numero_conta, tipo_pessoa)
SELECT '0001', '00000001', 'PJ'
WHERE NOT EXISTS (SELECT 1 FROM account WHERE numero_agencia = '0001' AND numero_conta = '00000001');
