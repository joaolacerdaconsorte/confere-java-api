# Roteiro do Pitch - Confere (5 minutos)

Roteiro preparado pelo grupo **Confere** (`2TDSPW`) seguindo os blocos do edital de Java Advanced (Global Solution 2026/2 - CitizenTech).

## 0:00 - 0:45 | O problema
- **Quem é afetado:** idosos, pessoas com baixo letramento digital e suas famílias.
- **Dados reais:** 16,5 milhões de brasileiros perderam R$ 21,2 bilhões em golpes digitais em 12 meses (GASA 2026); 82% dos idosos de SP já foram abordados por fraude digital (Fundação Seade). Ao mesmo tempo, 7,7 milhões de famílias têm direito à Tarifa Social de Energia e não utilizam (Aneel), e R$ 4,31 bilhões seguem esquecidos no Sistema Valores a Receber (Banco Central).
- **Dor central:** golpe parece mensagem oficial, e aviso oficial parece golpe.

## 0:45 - 1:30 | A solução
- O **Confere** coloca defesa contra golpes e acesso a direitos na mesma tela, com linguagem simples e leitura em voz alta.
- O cidadão envia um texto, print ou foto de conta/carta e recebe um veredito em semáforo: **GOLPE** (explica em até 3 frases e avisa o familiar guardião), **SUSPEITO**, **OFICIAL** ou **DIREITO** (mostra o benefício e o passo a passo).
- Funciona também sem internet com regras locais e fila de sincronização.

## 1:30 - 3:00 | Demonstração rodando
- Login no app Mobile integrado à API Java publicada na Azure (`https://confere-java-rm564969.azurewebsites.net`).
- Envio da mensagem da falsa prova de vida do INSS (`POST /api/verificacoes`): mascaramento de CPF/telefone (RN01), análise, explicação em até 3 frases pelo Spring AI (RN05) e publicação no RabbitMQ (`confere.verificacao.risco`) alertando o guardião João (RN06/RN07).
- Registro do feedback "Não paguei" e consulta do histórico com opção de exclusão em cascata (LGPD, RN09).
- Integração com a API .NET (`https://confere-rm564969.azurewebsites.net`) usando o mesmo token JWT HS256 para denúncias no Radar e avaliação de direitos.

## 3:00 - 4:00 | Modelo de negócio
- **Cidadão:** sempre gratuito, sem anúncios intrusivos.
- **B2G:** licenciamento do painel regional de golpes para prefeituras, CRAS e Procons via Contrato Público para Solução Inovadora (Marco Legal das Startups).
- **B2B2C:** oferta white-label para bancos e cooperativas reduzirem prejuízo com reembolsos e atendimento de fraude.

## 4:00 - 5:00 | Impacto, ODS e fechamento
- **Métricas:** golpes evitados, valor protegido declarado no feedback, direitos solicitados/concedidos e tempo de resposta dos guardiões.
- **ODS atendidos:** ODS 10 (redução das desigualdades), ODS 4 (letramento digital na prática), ODS 11 (alerta comunitário e cidades mais seguras) e ODS 9 (inovação e IA aplicada ao serviço público).
- **Fechamento:** "Antes de clicar, pagar ou desistir de um direito: confere."
