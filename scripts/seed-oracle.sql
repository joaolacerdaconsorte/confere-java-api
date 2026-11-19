-- ============================================================================
-- CONFERE - Seed inicial (Oracle) - Catálogos da Seção 5
-- ============================================================================

INSERT INTO CF_MUNICIPIO (NOME, UF, COD_IBGE) VALUES ('São Paulo', 'SP', 3550308);
INSERT INTO CF_MUNICIPIO (NOME, UF, COD_IBGE) VALUES ('Rio de Janeiro', 'RJ', 3304557);
INSERT INTO CF_MUNICIPIO (NOME, UF, COD_IBGE) VALUES ('Belo Horizonte', 'MG', 3106200);
INSERT INTO CF_MUNICIPIO (NOME, UF, COD_IBGE) VALUES ('Campinas', 'SP', 3509502);
INSERT INTO CF_MUNICIPIO (NOME, UF, COD_IBGE) VALUES ('Salvador', 'BA', 2927408);

INSERT INTO CF_CANAL_ORIGEM (CODIGO, NOME) VALUES ('WHATSAPP', 'WhatsApp');
INSERT INTO CF_CANAL_ORIGEM (CODIGO, NOME) VALUES ('SMS', 'SMS');
INSERT INTO CF_CANAL_ORIGEM (CODIGO, NOME) VALUES ('LIGACAO', 'Ligação telefônica');
INSERT INTO CF_CANAL_ORIGEM (CODIGO, NOME) VALUES ('EMAIL', 'E-mail');
INSERT INTO CF_CANAL_ORIGEM (CODIGO, NOME) VALUES ('CARTA', 'Carta ou boleto físico');
INSERT INTO CF_CANAL_ORIGEM (CODIGO, NOME) VALUES ('REDE_SOCIAL', 'Rede social');

INSERT INTO CF_TIPO_GOLPE (CODIGO, NOME, DESCRICAO, GRAVIDADE, ORGAO_IMITADO) VALUES
('FALSA_PROVA_VIDA', 'Falsa prova de vida (pede selfie ou documento)', 'Golpistas pedem selfie ou foto de documentos pelo WhatsApp simulando o INSS.', 5, 'INSS');
INSERT INTO CF_TIPO_GOLPE (CODIGO, NOME, DESCRICAO, GRAVIDADE, ORGAO_IMITADO) VALUES
('FALSO_REEMBOLSO_INSS', 'Falsa devolução de descontos do INSS', 'Promessa falsa de devolução imediata mediante clique em link ou pagamento de taxa.', 5, 'INSS');
INSERT INTO CF_TIPO_GOLPE (CODIGO, NOME, DESCRICAO, GRAVIDADE, ORGAO_IMITADO) VALUES
('FALSA_CENTRAL', 'Falsa central do banco', 'Ligação ou mensagem fingindo ser a central de segurança do banco.', 5, 'Bancos');
INSERT INTO CF_TIPO_GOLPE (CODIGO, NOME, DESCRICAO, GRAVIDADE, ORGAO_IMITADO) VALUES
('FALSO_PARENTE', '"Oi mãe, troquei de número"', 'Pessoa se passa por familiar pedindo transferência via Pix com urgência.', 4, 'Família');
INSERT INTO CF_TIPO_GOLPE (CODIGO, NOME, DESCRICAO, GRAVIDADE, ORGAO_IMITADO) VALUES
('FALSO_VALORES_RECEBER', 'Falso resgate de valores esquecidos', 'Site falso cobrando taxa para liberar valores do Sistema Valores a Receber.', 4, 'Banco Central');
INSERT INTO CF_TIPO_GOLPE (CODIGO, NOME, DESCRICAO, GRAVIDADE, ORGAO_IMITADO) VALUES
('FALSO_EMPRESTIMO', 'Empréstimo consignado falso ou não autorizado', 'Oferta enganosa de consignado ou pedido de senha/selfie para cancelar empréstimo.', 4, 'Bancos / INSS');
INSERT INTO CF_TIPO_GOLPE (CODIGO, NOME, DESCRICAO, GRAVIDADE, ORGAO_IMITADO) VALUES
('DEEPFAKE_CELEBRIDADE', 'Vídeo falso de jornalista ou famoso', 'Vídeo manipulado por IA prometendo benefício ou indenização.', 4, 'Imprensa');
INSERT INTO CF_TIPO_GOLPE (CODIGO, NOME, DESCRICAO, GRAVIDADE, ORGAO_IMITADO) VALUES
('FALSO_BOLETO', 'Boleto ou conta adulterada', 'Boleto com código de barras ou QR Code Pix alterado.', 3, 'Concessionárias');
INSERT INTO CF_TIPO_GOLPE (CODIGO, NOME, DESCRICAO, GRAVIDADE, ORGAO_IMITADO) VALUES
('LINK_RASTREIO', 'Falsa taxa de entrega ou rastreio', 'SMS ou e-mail cobrando taxa para liberar encomenda.', 3, 'Correios');
INSERT INTO CF_TIPO_GOLPE (CODIGO, NOME, DESCRICAO, GRAVIDADE, ORGAO_IMITADO) VALUES
('PIX_ERRADO', '"Te mandei um Pix por engano"', 'Pedido de devolução de Pix falso para outra conta.', 3, NULL);

INSERT INTO CF_REGRA_OFFLINE (PADRAO, TIPO_SINAL, PESO, MENSAGEM, VERSAO, ATIVO) VALUES
('(?i)(selfie|foto do rosto|biometria facial)', 'PEDIDO_SELFIE', 0.55, 'Pedido de selfie ou biometria por mensagem é sinal clássico de golpe.', 1, 'S');
INSERT INTO CF_REGRA_OFFLINE (PADRAO, TIPO_SINAL, PESO, MENSAGEM, VERSAO, ATIVO) VALUES
('(?i)(senha|token|código de segurança|pin)', 'PEDIDO_SENHA', 0.60, 'Órgãos públicos e bancos nunca pedem sua senha ou token.', 1, 'S');
INSERT INTO CF_REGRA_OFFLINE (PADRAO, TIPO_SINAL, PESO, MENSAGEM, VERSAO, ATIVO) VALUES
('(?i)(vence hoje|último dia|bloqueado|suspenso|urgente)', 'URGENCIA', 0.30, 'Pressa exagerada para agir hoje costuma ser usada para enganar.', 1, 'S');
INSERT INTO CF_REGRA_OFFLINE (PADRAO, TIPO_SINAL, PESO, MENSAGEM, VERSAO, ATIVO) VALUES
('(?i)(bit\\.ly|tinyurl|\\.xyz|\\.top|clique aqui)', 'LINK_SUSPEITO', 0.35, 'Links encurtados ou fora do domínio gov.br exigem atenção.', 1, 'S');
INSERT INTO CF_REGRA_OFFLINE (PADRAO, TIPO_SINAL, PESO, MENSAGEM, VERSAO, ATIVO) VALUES
('(?i)(pix|taxa de liberação|pague agora|transferência)', 'CHAVE_PIX', 0.35, 'Cobrança via Pix para liberar benefício ou entrega é suspeita.', 1, 'S');
INSERT INTO CF_REGRA_OFFLINE (PADRAO, TIPO_SINAL, PESO, MENSAGEM, VERSAO, ATIVO) VALUES
('(?i)(inss|receita federal|banco central|correios|gov\\.br)', 'ORGAO_OFICIAL_CITADO', 0.15, 'A mensagem cita órgão oficial: confirme sempre pelo canal oficial.', 1, 'S');

COMMIT;
