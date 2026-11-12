# Carrinho Certo

App Android nativo em português do Brasil para conferir a compra no supermercado. Sem login e anúncios. Kotlin, Compose Material 3, CameraX, ML Kit com modelos embarcados, Room/Flow, Koin, Retrofit e kotlinx.serialization. Android 8 (API 26) ou superior; target/compile SDK 35; JDK 17.

## Versão 1.1 — visual e saldo disponível

Identidade verde com tema claro/escuro, ícones vetoriais próprios, novo ícone do aplicativo, cartões de produtos e histórico, total animado e scanner compacto. A opção Privacidade está no ícone de escudo no cabeçalho.

Toque em “Quanto você quer gastar?” no cartão do total e informe o valor da compra. O orçamento aceita `200` ou `200,00` como **R$ 200,00**. O **saldo disponível = limite − total** é recalculado ao adicionar, remover, alterar preço ou quantidade, inclusive por kg. Ao ultrapassar o limite, o saldo fica negativo e há um aviso; a partir de 80% o total recebe destaque amarelo. O limite permanece salvo até ser alterado ou removido, e volta ao valor integral ao iniciar um carrinho vazio. A convenção de reais inteiros é exclusiva do orçamento; a entrada de preço de produto continua aceitando dígitos como centavos, com explicação no formulário.

Instale o APK 1.1 sobre a versão 1.0 para preservar dados. O applicationId, a assinatura de debug e a versão do banco Room permanecem os mesmos; não é necessário desinstalar o app. O versionCode foi incrementado para 2.

## Abrir e compilar

Abra esta pasta no Android Studio compatível com AGP 8.9.1. Instale Android SDK Platform 35 e Build Tools 36.0.0 pelo SDK Manager. Selecione JDK 17 em Settings → Build Tools → Gradle. Aguarde a sincronização. A primeira compilação precisa de internet para baixar dependências; depois o scanner e OCR funcionam offline.

Crie `local.properties` (não versionado) com o caminho do SDK, por exemplo:

```properties
sdk.dir=C:/Users/SEU_USUARIO/AppData/Local/Android/Sdk
```

No terminal, com `JAVA_HOME` apontando para o JDK 17:

```sh
./gradlew testDebugUnitTest assembleDebug
```

No PowerShell, use `./gradlew.bat testDebugUnitTest assembleDebug`. APK: `app/build/outputs/apk/debug/app-debug.apk`. Relatório de testes: `app/build/reports/tests/testDebugUnitTest/index.html`.

## Instalar via USB

Ative as opções do desenvolvedor e a depuração USB no celular, conecte-o e aceite a autorização RSA. Com `platform-tools` no PATH:

```sh
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Ou execute pelo botão Run do Android Studio. Teste câmera, lanterna, beep, vibração e leitura em um aparelho físico. Se a permissão for negada, o app explica a finalidade e permite abrir configurações ou digitar manualmente.

## APK de release assinado

No Android Studio, use Build → Generate Signed App Bundle / APK → APK. Crie ou selecione seu keystore, alias e senhas, escolha `release` e conclua o assistente. Guarde o keystore e as credenciais em local seguro, fora do Git: serão necessários nas atualizações. O assistente informa o destino do APK assinado (normalmente `app/release/`).

Para assinatura pela linha de comando, gere o APK e use as ferramentas do SDK; senhas serão solicitadas interativamente:

```sh
./gradlew assembleRelease
keytool -genkeypair -keystore carrinho-release.jks -alias carrinho -keyalg RSA -keysize 3072 -validity 10000
zipalign -p -f 4 app/build/outputs/apk/release/app-release-unsigned.apk app-release-aligned.apk
apksigner sign --ks carrinho-release.jks --ks-key-alias carrinho --out carrinho-certo.apk app-release-aligned.apk
apksigner verify --verbose carrinho-certo.apk
```

`zipalign` e `apksigner` ficam em `<SDK>/build-tools/36.0.0/`; no Windows use `apksigner.bat`. Não publique APK debug. O projeto fixa Build Tools 36.0.0 para usar a ferramenta já instalada no ambiente de validação, mantendo compile/target SDK em 35.

## Uso e decisões

- Primeiro escolha/crie o mercado. O nome normalizado é seu identificador; escreva o mesmo nome/bairro para usar preços compartilhados do mesmo estabelecimento. Não solicitamos localização. Para trocar de mercado, finalize ou esvazie o carrinho.
- Uma leitura vibra e emite beep; o mesmo código é ignorado por dois segundos. Sempre há confirmação de nome, preço e quantidade, seguindo o pedido ampliado do anexo. Preço recente (menos de 30 dias) é preenchido; preço antigo solicita etiqueta, mas continua editável.
- Ao reler um item, a quantidade atual aparece. A opção “Substituir quantidade atual” vem marcada; desmarque para somar uma quantidade adicional. Correções de preço no carrinho usam essa mesma confirmação.
- Dinheiro é `Long` em centavos. Quantidade é `BigDecimal`, persistida como texto exato. Unidade aceita apenas inteiros; kg aceita três casas decimais. Cada subtotal é arredondado em centavos com HALF_UP antes da soma. Limites: preço de 1 a 999.999.999 centavos e quantidade até 9999.
- Entrada monetária: `12,90` ou `12.90` = R$ 12,90; sem separador, `1290` = R$ 12,90, inclusive `12` = R$ 0,12. Essa convenção é exibida no formulário. Para R$ 12,00 digite `12,00`.
- Botões de kg alteram 0,100 kg; toque na quantidade para digitar outro peso. Chegar a zero remove. Deslizar ou “Remover” oferece desfazer.
- Preços são por mercado. Produtos e metadados são comuns ao aparelho. A última compra mostra preço anterior quando diferente; o histórico preserva nome, unidade, preço e quantidade originais.
- Excluir um produto apaga seus preços salvos e preserva carrinho/histórico; o carrinho exibe nome genérico se necessário. Deixar o preço vazio ao editar apaga apenas o preço do mercado atual. Editar produto salvo afeta próximas leituras; o carrinho tem sua própria correção de preço.
- Orçamento e mercado ficam em SharedPreferences; carrinho e compras ficam em Room. Compra finalizada é uma transação: histórico e limpeza são atômicos. Rotação não perde o carrinho. Um formulário de confirmação ainda não salvo pode ser cancelado quando o processo do app for encerrado.
- Tema acompanha o sistema. Total e ações ficam fixos acima da lista rolável; telas menores podem exigir rolagem no formulário. Tela principal em retrato é a experiência recomendada.

## OCR de etiquetas

O botão “Foto da etiqueta” processa quadros locais com ML Kit Latin. “Capturar preço” analisa o próximo quadro, sem gravar foto. Também sugere após três quadros consecutivos com o mesmo candidato. Oferece até três alternativas, sempre exigindo toque e confirmação final.

`PriceTagParser` é Kotlin puro, com testes parametrizados (31 etiquetas), regex brasileira, destaque por altura, centralidade, `R$` e preferência por “por”. Penaliza “de”, clube e atacado; ignora percentuais, gramaturas e datas reconhecíveis. Etiquetas por 100 g são convertidas para kg. OCR não detecta traços de preço riscado de forma confiável e pode confundir preços promocionais condicionais: confira sempre preço, unidade e condições da oferta. Se a leitura falhar, digite manualmente. Nenhuma imagem de câmera é salva ou enviada.

## Estrutura

Um módulo `app`, com `data` (entidades, DAO, banco, API, repositórios, worker), `domain` (dinheiro, quantidade, debounce e parser) e `ui` (Compose, scanner e ViewModel com StateFlow). A composição de dependências Koin fica em `CarrinhoApplication`. Histórico usa snapshots; incremento e finalização usam transações Room. Schemas Room são exportados em `app/schemas`.

## Supabase opcional

O app funciona integralmente sem servidor. A configuração apenas disponibiliza a opção; o compartilhamento permanece desligado até ser habilitado em Privacidade. Em `local.properties`:

```properties
supabase.url=https://SEU_PROJETO.supabase.co
supabase.anonKey=SUA_CHAVE_PUBLICA_ANON
```

Nunca use `service_role`. `local.properties` não é versionado; a chave pública anon fica necessariamente no APK e depende de RLS e limites no servidor. A interface `PriceRepository` tem implementações local e remota REST. Falhas nas consultas remotas permitem continuar offline. Confirmações com compartilhamento ativo enfileiram WorkManager com conectividade e retentativas exponenciais limitadas. Desligar o compartilhamento impede workers pendentes de enviar. Upload em andamento pode terminar.

SQL inicial para um ambiente de desenvolvimento:

```sql
create table public.prices (
  id bigint generated always as identity primary key,
  ean text not null check (length(ean) between 1 and 80),
  store_id text not null check (length(store_id) between 1 and 200),
  cents bigint not null check (cents between 1 and 999999999),
  unit text not null check (unit in ('un', 'kg')),
  updated_at bigint not null,
  device_id uuid not null,
  received_at timestamptz not null default now()
);
create index prices_lookup on public.prices (ean, store_id, updated_at desc);
create index prices_rate_lookup on public.prices (device_id, received_at desc);
alter table public.prices enable row level security;
grant select, insert on public.prices to anon;
grant usage, select on sequence public.prices_id_seq to anon;
create policy leitura_publica on public.prices for select to anon using (true);
create policy insercao_anonima on public.prices for insert to anon
with check (updated_at between
  (extract(epoch from now() - interval '30 days') * 1000)::bigint and
  (extract(epoch from now() + interval '5 minutes') * 1000)::bigint);

create function public.limit_price_insert() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  perform pg_advisory_xact_lock(hashtextextended(new.device_id::text, 0));
  if (select count(*) from public.prices
      where device_id = new.device_id and received_at > now() - interval '1 hour') >= 120 then
    raise exception 'Limite de envios atingido';
  end if;
  new.received_at := now();
  return new;
end;
$$;
create trigger price_rate_limit before insert on public.prices
for each row execute function public.limit_price_insert();
```

Esse limite por identificador evita envios acidentais excessivos; um cliente pode inventar outro identificador. RLS sozinho não oferece limite de taxa robusto para clientes anônimos. Antes de expor um serviço público, imponha quotas por IP no gateway/Edge Function e monitore abuso, custos e retenção. O endpoint REST direto implementado aqui é opcional e apropriado para servidor controlado/piloto. Não existem políticas de update/delete para `anon`; corrigir preço envia um novo registro, e o mais recente é consultado. “Esse preço está errado” é atendido pela edição do preço na confirmação. Não há autenticação nem mecanismo de reputação/moderação.

## Privacidade e rede

Apenas o EAN é consultado no Open Food Facts; metadados e foto retornados são armazenados/carregados. Preços locais não são enviados a essa API. Compartilhamento habilitado envia código, preço, unidade, mercado, data e UUID aleatório persistente ao Supabase. Câmera/OCR são locais; SDKs ML Kit podem enviar métricas técnicas ao Google. Sem analytics próprio. Backup Android está desativado. A tela Privacidade explica os fluxos e permite desligar o remoto. Apagar os dados do app remove o histórico local; não apaga registros antes compartilhados.

Referências: [compatibilidade AGP/Gradle](https://developer.android.com/build/releases/about-agp), [ML Kit Barcode Scanning](https://developers.google.com/ml-kit/vision/barcode-scanning/android), [ML Kit Text Recognition](https://developers.google.com/ml-kit/vision/text-recognition/v2/android).

## Validação neste ambiente

Em 11/11/2026, `gradlew.bat testDebugUnitTest assembleDebug --no-daemon --console=plain` concluiu com `BUILD SUCCESSFUL`: 37 testes, zero falhas (31 casos parametrizados do parser, 1 teste de ranking e 5 testes de regras). O APK foi inspecionado com `aapt`: nome Carrinho Certo, minSdk 26, targetSdk 35, quatro ABIs. Os modelos embarcados de código de barras/OCR e bibliotecas de quatro arquiteturas tornam o APK de debug aproximadamente 74 MiB.

Não havia dispositivo conectado por ADB. Câmera, lanterna, vibração, beep, comportamento em rotação e qualidade de reconhecimento precisam de teste funcional no celular. O servidor Supabase não estava configurado; não houve validação de integração com um servidor real. As consultas de rede têm fallback local, mas não substituem teste de campo com etiquetas e iluminação do supermercado.
