-- Tipo de arquivo deixa de ser um atributo do servico: passa a ser detectado
-- automaticamente a partir de cada arquivo enviado (arquivo-modelo ou documento a processar),
-- e o matching de termos passa a considerar todos os servicos ativos, independente do formato.
ALTER TABLE servico DROP COLUMN tipo_arquivo;
