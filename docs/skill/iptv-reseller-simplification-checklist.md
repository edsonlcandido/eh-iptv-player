# Roteiro â€” simplificaÃ§Ã£o do StreamVault para Eh!IPTV

## Objetivo

Usar este documento como checklist repetÃ­vel para transformar uma nova cÃ³pia do projeto base **StreamVault** em uma build simplificada de revenda chamada **Eh!IPTV**. Execute as fases na ordem, confirme cada item e sÃ³ marque a fase como concluÃ­da apÃ³s a verificaÃ§Ã£o correspondente.

Este roteiro consolida as alteraÃ§Ãµes implementadas atÃ© o commit atual (`57da4be simplificado configuraÃ§Ãµes`, branch `ehiptv/custom-and-simplify`), com ReproduÃ§Ã£o enxuta, categorias ocultas no rail, a seÃ§Ã£o Privacidade reduzida ao toggle de ConteÃºdo adulto + Limpar histÃ³rico, a seÃ§Ã£o Sobre limitada a VersÃ£o/Site/Agradecimento, a Fase 4f (VOD enxuto em Filmes e SÃ©ries), a Fase 4g (Detalhes VOD enxutos â€” somente Play/Chromecast/Favorito/Trailer), a Fase 4h (Player VOD enxuto â€” sem Quality/Audio/Stop Playback/Standby/PiP, e `Elenco` â†’ `TransmissÃ£o` em PT), a Fase 4i (Player IPTV enxuto â€” sem Subs/Audio/Rec/C-UP/Split/PiP; `Subscritores` â†’ `Legendas`; `TransmissÃ£o` â†’ `Chromecast` em PT) e a Fase 4j (ChannelInfoOverlay sem Quality no Live TV). A versÃ£o anterior estava alinhada atÃ© `817d3c9` (`Campo provedores alterado para Eh! IPTV`).

## Quando usar

- Ao iniciar a customizaÃ§Ã£o de uma nova versÃ£o/clone do StreamVault.
- Ao reaplicar a personalizaÃ§Ã£o depois de atualizar o projeto base.
- Ao revisar uma build antes de entregar o APK ao revendedor.

## Fase 0 â€” Preparar o projeto base

1. Crie uma branch de customizaÃ§Ã£o a partir do StreamVault:

```bash
git switch -c ehiptv/custom-and-simplify
```

2. Confirme o estado inicial e os commits aplicados:

```bash
git status --short
git log --oneline -10
```

3. Leia `AGENTS.md`, `README.md` e este roteiro antes de editar.
4. Preserve credenciais fora do Git. Use `local.properties` somente para dados de desenvolvimento.
5. ~~Depois de renomear pacotes, limpe os caches KSP~~ **NÃ£o se aplica** na arquitetura atual (skill #14 + `productFlavors`): o cÃ³digo-fonte nÃ£o Ã© renomeado, entÃ£o nÃ£o hÃ¡ invalidaÃ§Ã£o de KSP/Hilt. Se vocÃª mudou `BuildConfig` fields ou `namespace`, o build incremental Ã© suficiente. Apenas rode `./gradlew --stop` se o daemon estiver com classes velhas em cache (raro).

## Fase 1 â€” Identidade e pacote

> **Caminho novo (recomendado):** skill #14 + `productFlavors` no `app/build.gradle.kts`. As constantes de marca (URL Xtream, nome do provedor, URL do WhatsApp, cores, etc.) vivem como `buildConfigField` no bloco `create("ehtudo") { ... }`, expostas via `BuildConfig.*`. O `applicationId` e o `namespace` ficam no mesmo bloco.

- [ ] Existe um bloco `productFlavors { create("ehtudo") { ... } }` em `app/build.gradle.kts`, dentro de `flavorDimensions += "brand"`.
- [ ] O bloco define os campos: `applicationId`, `BRAND_NAME`, `XTREAM_DEFAULT_URL`, `XTREAM_DEFAULT_PROVIDER_NAME`, `WHATSAPP_URL`, `BRAND_PRIMARY_COLOR`, `BRAND_SECONDARY_COLOR`, `BRAND_DIM_COLOR`, `REMOTE_CONFIG_URL`, `SHOW_ADVANCED_OPTIONS`, `ENABLE_TV_INPUT_SERVICE`.
- [ ] O flavor `ehtudo` Ã© o **Ãºnico** declarado por enquanto. Outros resellers (se houver) viram `create("outromarket") { ... }` no mesmo bloco.
- [ ] Build debug adiciona o sufixo `.debug` (herdado do `buildTypes.debug.applicationIdSuffix = ".debug"`).
- [ ] Marca exibida ao usuÃ¡rio = `BuildConfig.BRAND_NAME` (ou `R.string.app_name` se vocÃª usar flavor resources).
- [ ] TÃ­tulo do app e Ã­cones foram atualizados (skill #13) sem alterar o contrato de mÃ³dulos.
- [ ] O nome do projeto Gradle (`settings.gradle.kts` `rootProject.name`) pode continuar `StreamVault`; ele Ã© o nome do *projeto*, nÃ£o do *aplicativo*, e nÃ£o precisa ser renomeado para funcionar.
- [ ] Confirme `versionName` e `versionCode` no `defaultConfig` (ou por flavor, se cada reseller tiver versÃ£o prÃ³pria).

> **Caminho antigo (apenas se vocÃª estÃ¡ numa base que ainda nÃ£o migrou pro `productFlavors`):** os checks antigos continuam vÃ¡lidos â€” `applicationId = "app.ehtudo.iptv"`, `namespace = "app.ehtudo.iptv"` (ou `com.streamvault.app`, com `applicationId` separado), constantes em `WelcomeScreen.kt:80` e `ProviderSetupScreen.kt:111`. Migre para `productFlavors` antes de adicionar o segundo reseller.

## Fase 2 â€” Welcome simplificado

Arquivos principais: `app/src/main/java/app/ehtudo/iptv/ui/screens/welcome/WelcomeScreen.kt` e `app/src/main/res/values/strings.xml`.

- [ ] TÃ­tulo = `R.string.welcome_brand_title` (`Eh! IPTV`).
- [ ] A tela inicial mostra somente dois campos: usuÃ¡rio e senha.
- [ ] Os dois campos sÃ£o texto simples: sem `PasswordVisualTransformation`, Ã­cone de olho ou `KeyboardType.Password`.
- [ ] O usuÃ¡rio e a senha sÃ£o validados antes da chamada de login.
- [ ] O botÃ£o usa `R.string.welcome_save` (`Salvar`).
- [ ] O botÃ£o Salvar usa fundo azul-claro (`AppColors.BrandStrong`) e texto branco.
- [ ] Logo abaixo do botÃ£o Salvar aparece um link `Fale conosco pelo WhatsApp (+5511932055173)` (`R.string.welcome_whatsapp_link`) com sublinhado e cor `AppColors.Brand`. Ao clicar, abre `http://wa.me/+5511932055173` em um `Intent.ACTION_VIEW` (constante `EH_IPTV_WHATSAPP_URL` em `WelcomeScreen.kt`). O nÃºmero Ã© exibido entre parÃªnteses para o usuÃ¡rio conseguir visualizar e discar manualmente em instalaÃ§Ãµes em TV (sem cÃ¢mera/leitor de QR).
- [ ] O login constrÃ³i `XtreamProviderSetupCommand` com:
  - `serverUrl = "http://dnstv.top/"`;
  - `name = "Eh! IPTV"`;
  - `xtreamFastSyncEnabled = true`.
- [ ] O login nÃ£o bloqueia a entrada aguardando a sincronizaÃ§Ã£o completa.
- [ ] A sincronizaÃ§Ã£o posterior ocorre em background.

## Fase 3 â€” Provider Setup avanÃ§ado

Arquivo principal: `app/src/main/java/app/ehtudo/iptv/ui/screens/provider/ProviderSetupScreen.kt`.

- [ ] O fluxo Xtream simplificado pede apenas usuÃ¡rio e senha.
- [ ] A URL Xtream padrÃ£o e o nome padrÃ£o sÃ£o aplicados automaticamente.
- [ ] NÃ£o hÃ¡ campo de servidor, nome de playlist ou opÃ§Ãµes avanÃ§adas no fluxo simplificado.
- [ ] M3U, Stalker e Jellyfin continuam acessÃ­veis pelo setup avanÃ§ado.
- [ ] O fluxo avanÃ§ado nÃ£o perde os campos especÃ­ficos de cada tipo.

## Fase 4 â€” ConfiguraÃ§Ãµes > Eh!IPTV

Arquivos principais:
- `app/src/main/java/app/ehtudo/iptv/ui/screens/settings/SettingsNavigationRail.kt`
- `app/src/main/java/app/ehtudo/iptv/ui/screens/settings/SettingsProviderSection.kt`
- `app/src/main/java/app/ehtudo/iptv/ui/screens/settings/SettingsViewModel.kt`
- `app/src/main/res/values/strings.xml`

- [ ] A categoria lateral que antes era `Providers/Provedores` exibe `Eh!IPTV` atravÃ©s de `R.string.settings_providers`.
- [ ] Quando jÃ¡ existem provedores, a tela mostra a seleÃ§Ã£o e o card de gerenciamento do provedor.
- [ ] A seÃ§Ã£o `Combined M3U` nÃ£o Ã© exibida nessa tela.
- [ ] O botÃ£o `Adicionar provedor` nÃ£o Ã© exibido nessa tela.
- [ ] Quando nÃ£o existe nenhum provedor, a tela mostra inline:
  - campo de usuÃ¡rio;
  - campo de senha em texto simples;
  - botÃ£o `Salvar`;
  - link `Fale conosco pelo WhatsApp (+5511932055173)` (`R.string.welcome_whatsapp_link`) abaixo do botÃ£o Salvar, com sublinhado e cor `AppColors.Brand`, abrindo `http://wa.me/+5511932055173` em `Intent.ACTION_VIEW` (constante `EH_IPTV_WHATSAPP_URL` em `SettingsProviderSection.kt`). O nÃºmero Ã© exibido entre parÃªnteses para o usuÃ¡rio visualizar e discar manualmente em instalaÃ§Ãµes em TV (sem cÃ¢mera/leitor de QR);
  - mensagem de erro de validaÃ§Ã£o ou autenticaÃ§Ã£o.
- [ ] O formulÃ¡rio vazio usa o mesmo URL Xtream fixo `http://dnstv.top/` e nome padrÃ£o `Eh! IPTV` do Welcome.
- [ ] O botÃ£o Salvar do formulÃ¡rio de provedores usa fundo azul-claro (`AppColors.BrandStrong`) e texto branco.
- [ ] O formulÃ¡rio fica desabilitado durante a criaÃ§Ã£o/sincronizaÃ§Ã£o inicial.
- [ ] Editar, excluir, conectar, atualizar e controle parental continuam disponÃ­veis para provedores existentes.

## Fase 4b â€” ReproduÃ§Ã£o enxuta

Arquivos principais:
- `app/src/main/java/app/ehtudo/iptv/ui/screens/settings/SettingsPlaybackSection.kt`
- `app/src/main/java/app/ehtudo/iptv/ui/screens/settings/SettingsContentPane.kt`
- `app/src/main/res/values/strings.xml`

- [ ] A categoria **ReproduÃ§Ã£o/Playback** do rail sÃ³ exibe duas coisas: a linha `Live stream format` e o card de teste de velocidade (`InternetSpeedTestCard`).
- [ ] Nenhum toggle extra de decoder, timeshift, legendas, buffer, qualidade de rede, modo zap, compatibilidade, sincronizaÃ§Ã£o, Multiview ou sessÃ£o multimÃ­dia Ã© renderizado.
- [ ] A linha `Live stream format` continua abrindo o diÃ¡logo `PremiumSelectionDialog` com `AUTO`, `HLS` e `MPEG_TS`, persistindo em `viewModel.setPlayerLiveStreamFormatMode(...)`.
- [ ] O card de teste de velocidade continua mostrando o Ãºltimo resultado, o botÃ£o **Rodar teste**, e os botÃµes **Aplicar ao Wi-Fi** e **Aplicar ao cabo**, chamando `viewModel::runInternetSpeedTest`, `viewModel::applySpeedTestRecommendationToWifi` e `viewModel::applySpeedTestRecommendationToEthernet`.
- [ ] A assinatura de `settingsPlaybackSection(...)` foi enxugada para apenas `uiState`, `viewModel`, `lastSpeedTestLabel`, `lastSpeedTestSummary` e `speedTestRecommendationLabel`. Os demais labels e callbacks de diÃ¡logo podem ser removidos do call-site em `SettingsContentPane`.
- [ ] A string `settings_live_stream_format` existe em `values/strings.xml` e substitui o rÃ³tulo literal que existia dentro da seÃ§Ã£o.

## Fase 4c â€” Categorias ocultas no rail

Arquivo principal: `app/src/main/java/app/ehtudo/iptv/ui/screens/settings/SettingsNavigationRail.kt`.

- [ ] O rail lateral de ConfiguraÃ§Ãµes mostra apenas as quatro categorias: `Eh!IPTV`, `ReproduÃ§Ã£o/Playback`, `Privacidade/Privacy` e `Sobre/About`.
- [ ] As categorias a seguir nÃ£o sÃ£o renderizadas em nenhum estado:
  - NavegaÃ§Ã£o/Browsing.
  - GravaÃ§Ã£o/Recording.
  - Backup & Restore.
  - EPG Sources.
- [ ] O switch do `LazyColumn` em `SettingsContentPane.kt` consome apenas os Ã­ndices `0..3`; ramos para Ã­ndices maiores sÃ£o removidos.
- [ ] `SettingsScreen.kt` nÃ£o forÃ§a mais `dialogState.selectedCategory = 5` no caminho de import inicial; a inspeÃ§Ã£o de backup segue acontecendo, mas sem selecionar uma categoria oculta.
- [ ] Strings das categorias ocultas (`settings_browsing`, `settings_recording_title`, `settings_backup_restore`, `EPG Sources`) podem permanecer em `strings.xml` para evitar impacto em outros consumidores.

## Fase 4d â€” Privacidade enxuta (somente ConteÃºdo adulto + Limpar histÃ³rico)

Arquivos principais:
- `app/src/main/java/app/ehtudo/iptv/ui/screens/settings/SettingsPrivacySection.kt`
- `app/src/main/java/app/ehtudo/iptv/ui/screens/settings/SettingsUiStateModel.kt`
- `app/src/main/java/app/ehtudo/iptv/ui/screens/settings/SettingsViewModel.kt`
- `app/src/main/java/app/ehtudo/iptv/ui/screens/settings/SettingsContentPane.kt`
- `app/src/main/java/app/ehtudo/iptv/ui/screens/settings/SettingsPreferenceSnapshotMapper.kt`
- `app/src/main/res/values/strings.xml` e `values-pt/strings.xml`

- [ ] A categoria `Privacidade` mostra apenas o toggle `ConteÃºdo adulto` e o card `Limpar histÃ³rico de visualizaÃ§Ã£o`. **NÃ£o hÃ¡** item de configuraÃ§Ã£o manual de PIN â€” o PIN padrÃ£o `0000` Ã© fixo e gravado automaticamente na primeira ativaÃ§Ã£o.
- [ ] O toggle `ConteÃºdo adulto` (chave `settings_adult_content`) controla um Ãºnico `Switch` que altera `viewModel.setAdultContentEnabled(...)`.
- [ ] Quando o toggle estÃ¡ **desligado**, a Ã¡rea Ã  direita do tÃ­tulo mostra `OCULTO` (chave `settings_adult_content_status_hidden`) e o nÃ­vel de proteÃ§Ã£o (`parentalControlLevel`) Ã© `3` (HIDDEN) â€” o conteÃºdo adulto nÃ£o aparece em nenhuma lista do app.
- [ ] Quando o toggle estÃ¡ **ligado**, a Ã¡rea Ã  direita do tÃ­tulo mostra `BLOQUEADO` (chave `settings_adult_content_status_locked`) e o nÃ­vel de proteÃ§Ã£o Ã© `1` (LOCKED) â€” o conteÃºdo adulto aparece nas listas, mas exige o PIN para abrir cada categoria.
- [ ] Na primeira ativaÃ§Ã£o, o app grava o PIN padrÃ£o `0000` (constante `DEFAULT_ADULT_CONTENT_PIN` em `SettingsViewModel.kt`) e marca `hasParentalPin = true`. AtivaÃ§Ãµes subsequentes preservam o PIN jÃ¡ configurado pelo usuÃ¡rio.
- [ ] O literal `0000` nunca Ã© exibido na interface. O subtitle do toggle descreve apenas a funÃ§Ã£o, sem mencionar o PIN.
- [ ] O item `Limpar histÃ³rico de visualizaÃ§Ã£o` continua abrindo o diÃ¡logo existente (`showClearHistoryDialog`) que chama `viewModel.clearHistory()`.
- [ ] NÃ£o devem ser renderizados em `settingsPrivacySection(...)`: `ParentalControlCard` (nÃ­veis OFF/LOCKED/PRIVATE/HIDDEN + alterar PIN), toggles de IncÃ³gnito, Xtream name-based adult detection e Xtream Base64 compatibility, e qualquer item de "Configurar PIN" / "Alterar PIN" para o PIN adulto.
- [ ] Strings e funÃ§Ãµes legadas (`settings_incognito_mode`, `settings_xtream_text_classification`, `settings_xtream_base64_compatibility`, `toggleIncognitoMode`, `toggleXtreamTextClassification`, `toggleXtreamBase64TextCompatibility`, `ParentalControlCard`, `ParentalAction`) podem permanecer no cÃ³digo desde que nÃ£o sejam referenciadas pela seÃ§Ã£o de Privacidade. Outros consumidores nÃ£o devem quebrar.
- [ ] `SettingsPreferenceSnapshotMapper.kt` define `adultContentEnabled = parentalControlLevel == 1 || parentalControlLevel == 2` (LOCKED ou PRIVATE â€” em ambos o conteÃºdo adulto aparece e exige PIN). Toggle OFF â†” nÃ­vel 3 (HIDDEN); toggle ON â†” nÃ­vel 1 (LOCKED).
- [ ] `SettingsViewModel.kt` define as constantes `PARENTAL_LEVEL_LOCKED = 1` e `PARENTAL_LEVEL_HIDDEN = 3` para que `setAdultContentEnabled(true)` mapeie para LOCKED e `setAdultContentEnabled(false)` mapeie para HIDDEN.
- [ ] A assinatura pÃºblica de `settingsPrivacySection(...)` foi enxugada para apenas `uiState`, `viewModel` e `onShowClearHistoryDialogChange`. Os callbacks `onShowPinDialogChange`/`onPendingActionChange` foram removidos do call-site em `SettingsContentPane`.
- [ ] Strings em `values/strings.xml`: `settings_adult_content`, `settings_adult_content_subtitle`, `settings_adult_content_status_hidden` (= `OCULTO`), `settings_adult_content_status_locked` (= `BLOQUEADO`). Em `values-pt/strings.xml`: as traduÃ§Ãµes equivalentes em portuguÃªs. As strings `settings_adult_content_status_configured`/`settings_adult_content_status_configure`/`settings_adult_content_configure_pin`/`settings_adult_content_configure_pin_subtitle` e qualquer menÃ§Ã£o visÃ­vel a `0000` foram removidas.

## Fase 4e â€” Sobre enxuto (somente VersÃ£o, Site e Agradecimento)

Arquivos principais:
- `app/src/main/java/app/ehtudo/iptv/ui/screens/settings/SettingsBackupAboutSections.kt`
- `app/src/main/java/app/ehtudo/iptv/ui/screens/settings/SettingsContentPane.kt`
- `app/src/main/res/values/strings.xml` e `values-pt/strings.xml`

- [ ] A categoria `Sobre` mostra apenas trÃªs linhas: `VersÃ£o do aplicativo`, `Site` (clicÃ¡vel) e `Agradecimento` (clicÃ¡vel).
- [ ] `VersÃ£o do aplicativo` exibe `${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})`.
- [ ] `Site` mostra `https://iptv.ehtudo.app/` e abre o URL no `onOpenUri(...)`. Constante `EH_IPTV_SITE_URL` em `SettingsBackupAboutSections.kt`.
- [ ] `Agradecimento` mostra `https://github.com/Davidona/StreamVault-IPTV` e abre o URL. Constante `STREAMVAULT_REPO_URL` no mesmo arquivo.
- [ ] SeÃ§Ãµes removidas: AtualizaÃ§Ãµes (auto-check, auto-download, latest release, status, last checked, check now, download, view release, error), Crash Reports e Build info (build, build verification, developed by, GitHub, donate).
- [ ] Assinatura de `settingsAboutSection(...)` foi enxugada para apenas `onOpenUri`. Os callbacks removidos ficam disponÃ­veis na assinatura de `SettingsContentPane` caso outros fluxos queiram reaproveitar, mas nada os referencia apÃ³s esta fase.
- [ ] Imports nÃ£o usados em `SettingsBackupAboutSections.kt` (`LaunchedEffect`, `AppUpdateActionState`) foram removidos.
- [ ] Strings novas em `values/strings.xml`: `settings_site`, `settings_site_url`, `settings_acknowledgment`, `settings_acknowledgment_url`. Em `values-pt/strings.xml`: `settings_site`, `settings_site_url`, `settings_acknowledgment` (= `Agradecimento`), `settings_acknowledgment_url`.

## Fase 4f â€” VOD enxuto (somente prateleiras em Filmes e SÃ©ries)

Arquivos principais:
- `app/src/main/java/app/ehtudo/iptv/ui/screens/movies/MoviesScreen.kt`
- `app/src/main/java/app/ehtudo/iptv/ui/screens/series/SeriesScreen.kt`

- [ ] A pÃ¡gina de **Filmes** mostra apenas as prateleiras (em ordem): Continuar assistindo â†’ Favoritos â†’ Mais recentes â†’ Mais bem avaliados â†’ Categorias.
- [ ] A pÃ¡gina de **SÃ©ries** mostra apenas as prateleiras equivalentes (na mesma ordem, com tÃ­tulos traduzidos via `library_lens_fresh_series`).
- [ ] O `hero` strip (`VodHeroStrip`) **nÃ£o** Ã© renderizado na `LazyColumn` do modo preview (default) â€” o bloco `item(key = "hero") { ... }` foi removido.
- [ ] A linha de pÃ­lulas (`VodActionChipRow` com `browse_all` / `categories` / `favorites` / `resume` / `top_rated` / `fresh`) **nÃ£o** Ã© renderizada no topo â€” o bloco `item(key = "actions") { ... }` foi removido.
- [ ] O `MoviesVodClassicContent` e o `SeriesVodClassicContent` (modo CLASSIC, `VodViewMode.CLASSIC`) **podem** continuar usando `VodActionChipRow` para os filtros de Browse/Filter â€” esses vivem abaixo do `VodClassicContentHeader` e sÃ£o independentes do chrome superior.
- [ ] As variÃ¡veis `heroMovie`/`heroSeries` (derivadas de `freshMovies/favoriteMovies`) foram removidas das duas telas; nÃ£o hÃ¡ mais fallback `if (hero == null)`.
- [ ] `fallbackMovieId`/`fallbackSeriesId` Ã© derivado diretamente da primeira prateleira nÃ£o-vazia (`favoriteMovies` â†’ `freshMovies` â†’ `topRatedMovies` â†’ `catEntries`).
- [ ] O `initialFocusRequester` foi movido do hero para o primeiro card da primeira prateleira visÃ­vel. PadrÃ£o adotado em `favorites_row`, `fresh_row`, `top_rated_row` e em `catEntries`: `Modifier.then(if (movie.id == fallbackMovieId) Modifier.focusRequester(initialFocusRequester) else Modifier)` (e a variante com `.width(favoriteCardWidth)` no `favorites_row`). `FocusRestoreHost` continua sendo acionado normalmente.
- [ ] O import `VodHeroStrip` foi removido de `MoviesScreen.kt` e `SeriesScreen.kt` (nenhum uso restante apÃ³s a remoÃ§Ã£o do hero).
- [ ] Os imports `VodActionChipRow` e `VodActionChip` continuam presentes porque o modo CLASSIC e o `VodCategoryPickerDialog` (acionado a partir das pÃ­lulas clÃ¡ssicas) ainda os utilizam.
- [ ] Os composables `VodHeroStrip` e `VodActionChipRow` em `app/src/main/java/app/ehtudo/iptv/ui/components/shell/VodChrome.kt` permanecem intactos â€” outros consumidores continuam reaproveitando-os.
- [ ] Strings usadas apenas pelo hero/pÃ­lulas removidos (`library_full_browse_title_movies`, `library_full_browse_title_series`, `library_full_browse_subtitle`, `movies_categories_title`, `series_categories_title`, `library_lens_continue`, `library_lens_top_rated`, `library_lens_fresh_movies`, `library_lens_fresh_series`, `favorites_title`, `library_saved_items_count`, `movies_library_lens_subtitle`, `series_library_lens_subtitle`, `player_resume`) podem permanecer em `values/strings.xml` e `values-pt/strings.xml` enquanto outros fluxos (modo CLASSIC, picker de categoria, histÃ³rico de continue watching) as referenciarem. NÃ£o hÃ¡ strings exclusivas do hero/pÃ­lulas que precisem ser apagadas.
- [ ] O build `:app:compileDebugKotlin` passa apÃ³s a mudanÃ§a (rodar `./gradlew :app:compileDebugKotlin --no-daemon`). Warnings prÃ©-existentes sobre condiÃ§Ãµes tautolÃ³gicas em `locked && matchedCategory != null` podem continuar presentes â€” nÃ£o foram introduzidos por esta fase.

## Fase 4g â€” Detalhes VOD enxutos (somente Play / Chromecast / Favorito / Trailer quando houver)

Arquivos principais:
- `app/src/main/java/app/ehtudo/iptv/ui/screens/movies/MovieDetailScreen.kt`
- `app/src/main/java/app/ehtudo/iptv/ui/screens/series/SeriesDetailScreen.kt`

- [ ] A linha de aÃ§Ãµes do **Filme** mostra, nesta ordem: `Play`/`Resume from â€¦` â†’ `Chromecast` â†’ `Trailer` (somente se `hasTrailer`) â†’ Favorito (heart).
- [ ] A linha de aÃ§Ãµes da **SÃ©rie** (em `SeriesDetailActions`, exibida quando hÃ¡ `resumeEpisode`) mostra, nesta ordem: `Resume Â· â€¦`/`Play Â· â€¦` â†’ `Chromecast` â†’ Favorito.
- [ ] Cada **episÃ³dio** (`EpisodeItem`) renderiza apenas o card `EpisodeRowCard` clicÃ¡vel e o botÃ£o `Chromecast`. **NÃ£o hÃ¡** botÃµes `Download` ou `Copy URL` no card de episÃ³dio.
- [ ] `Copy URL` (`R.string.stream_url_copy`) e `Download` (`R.string.download_button_label`) foram removidos da UI das telas de detalhe â€” eles nÃ£o aparecem em nenhuma das trÃªs aÃ§Ãµes (filme / sÃ©rie / episÃ³dio).
- [ ] Os parÃ¢metros `onCopyUrl`, `onDownload`, `onCopyEpisodeUrl`, `onDownloadEpisode` foram removidos de `MovieDetailContent`, `MovieDetailHeroText`, `SeriesDetailContent`, `SeriesDetailActions` e `EpisodeItem`.
- [ ] O helper `copyStreamUrlToClipboard(...)` foi removido de `MovieDetailScreen.kt` e `SeriesDetailScreen.kt` (nÃ£o hÃ¡ mais chamadores); os imports `android.content.ClipData`, `android.content.ClipboardManager`, `kotlinx.coroutines.launch`, `androidx.compose.runtime.rememberCoroutineScope`, `app.ehtudo.domain.model.Result` foram removidos das duas telas.
- [ ] As lambdas `coroutineScope.launch { copyStreamUrlToClipboard(...) }` e o wrapper `copyEpisodeUrl` foram removidos. `val coroutineScope = rememberCoroutineScope()` e `val copyEpisodeUrl: (Episode) -> Unit = ...` tambÃ©m.
- [ ] O foco inicial do `MovieDetailScreen` permanece no botÃ£o `Play` (`playButtonFocusRequester.requestFocusSafely(...)`).
- [ ] `viewModel.resolveCopyStreamUrl(...)` / `viewModel.downloadMovie(...)` / `viewModel.downloadEpisode(...)` podem permanecer no ViewModel mesmo sem chamadores na UI â€” nÃ£o hÃ¡ remoÃ§Ã£o no escopo desta fase.
- [ ] `MovieDetailViewModelCastingTest` e `SeriesDetailViewModelCastingTest` continuam passando (`./gradlew :app:testDebugUnitTest --no-daemon`). Esta fase nÃ£o altera a superfÃ­cie do ViewModel, entÃ£o nÃ£o hÃ¡ testes novos.
- [ ] Strings (`stream_url_copy`, `download_button_label`, `stream_url_clip_label`, `stream_url_copied`, `stream_url_copy_failed`) podem permanecer em `values/strings.xml` e `values-pt/strings.xml` â€” outros fluxos (ex.: `AddToGroupDialog`, `ContinueWatching`) podem referenciÃ¡-las futuramente.

## Fase 4h â€” Player VOD enxuto (somente aÃ§Ãµes essenciais no overlay de Filmes/SÃ©ries)

Arquivos principais:
- `app/src/main/java/app/ehtudo/iptv/ui/screens/player/overlay/PlayerControlsChrome.kt` (apenas o composable privado `PlayerVodInfo` e o call-site que o invoca a partir de `PlayerBottomBar`).
- `app/src/main/res/values-pt/strings.xml`.

- [ ] A linha de aÃ§Ãµes rÃ¡pidas do **player de Filmes/SÃ©ries** (`PlayerVodInfo`) mostra, nesta ordem: `Mute/Unmute` â†’ `Legendas` (se `subtitleTrackCount > 0`) â†’ `EpisÃ³dios` (se `showEpisodesAction`, apenas sÃ©ries) â†’ `External Player` (se `showExternalPlayerAction`) â†’ `Velocidade` (sempre) â†’ `A/V Sync` (se `audioVideoSyncEnabled && !isCastConnected`) â†’ `TransmissÃ£o`/`Parar TransmissÃ£o` (sempre) â†’ `ProporÃ§Ã£o (Fit/Stretch/Zoom)` (sempre).
- [ ] Os botÃµes **Qualidade de vÃ­deo**, **Ãudio**, **Parar reproduÃ§Ã£o apÃ³s**, **Permitir standby apÃ³s inatividade** e **Imagem em imagem** **nÃ£o** aparecem no player VOD. O servidor Xtream Eh! IPTV nÃ£o fornece mÃºltiplas trilhas de Ã¡udio/vÃ­deo nem usa esses timers; PiP tambÃ©m foi removido para evitar gravaÃ§Ã£o fora do app.
- [ ] O parÃ¢metro `sleepTimerUiState` foi removido de `PlayerVodInfo` (nÃ£o hÃ¡ mais timer visÃ­vel). `PlayerBottomBar` e `PlayerControlsOverlay` continuam recebendo `sleepTimerUiState` para uso do `PlayerLiveInfo` (Live TV nÃ£o foi alterado nesta fase).
- [ ] Os parÃ¢metros removidos de `PlayerVodInfo`: `audioTrackCount`, `videoQualityCount`, `sleepTimerUiState`, `onOpenAudioTracks`, `onOpenVideoTracks`, `onOpenStopPlaybackTimer`, `onOpenIdleStandbyTimer`, `onEnterPictureInPicture`. O call-site em `PlayerBottomBar` foi ajustado removendo apenas esses argumentos â€” `PlayerBottomBar` ainda os aceita para repassar ao `PlayerLiveInfo`.
- [ ] TraduÃ§Ã£o PT corrigida: `player_cast` e `player_action_cast` em `values-pt/strings.xml` mudaram de `Elenco` (que significa "elenco de atores") para `TransmissÃ£o` (termo correto para o recurso de cast/Chromecast). A variante EN permanece `Cast`/`Chromecast`. A variante `values-es/strings.xml` ainda tem `Elenco` mas nÃ£o foi corrigida nesta fase (PT Ã© o locale principal do revendedor).
- [ ] **Ressalva importante:** o `MainActivity.onUserLeaveHint()` (`app/src/main/java/app/ehtudo/iptv/MainActivity.kt:210-213`) ainda tenta entrar em PiP automaticamente quando o usuÃ¡rio sai do app durante a reproduÃ§Ã£o. Esta fase remove apenas o **botÃ£o** de PiP, nÃ£o o comportamento automÃ¡tico. RemovÃª-lo de fato exige mudar o manifesto, `MainActivity` e `enterPlayerPictureInPictureModeIfEligible` â€” fora do escopo desta fase.
- [ ] **Ressalva Live TV:** os mesmos botÃµes (`Ãudio`, `Qualidade`, `Stop Playback`, `Standby`, `PiP`) continuam aparecendo no overlay de Live TV (`PlayerLiveInfo` em `PlayerControlsChrome.kt:786-836` e no `PlayerChannelInfoOverlay`). Esta fase toca **somente** Filmes/SÃ©ries. Para aplicar a Live TV, abrir uma nova fase.
- [ ] `viewModel.selectAudioTrack(...)`, `viewModel.selectVideoQuality(...)`, `viewModel.setStopPlaybackTimer(...)`, `viewModel.setIdleStandbyTimer(...)`, `viewModel.enterPictureInPicture(...)` continuam existindo nos ViewModels/Actions. Esta fase remove apenas a UI â€” as APIs permanecem disponÃ­veis.
- [ ] Build `:app:compileDebugKotlin` e `:app:testDebugUnitTest` passam.

## Fase 4i â€” Player IPTV enxuto (Channel Info + Live controls sem Subs/Audio/Rec/C-UP/Split/PiP)

Arquivos principais:
- `app/src/main/java/app/ehtudo/iptv/ui/screens/player/overlay/PlayerChannelInfoOverlay.kt` â€” funÃ§Ã£o `ChannelInfoOverlay`.
- `app/src/main/java/app/ehtudo/iptv/ui/screens/player/overlay/PlayerControlsChrome.kt` â€” funÃ§Ã£o privada `PlayerLiveInfo` (Live branch do `PlayerBottomBar`).
- `app/src/main/java/app/ehtudo/iptv/ui/screens/player/PlayerScreen.kt` â€” call-site de `ChannelInfoOverlay`.
- `app/src/main/res/values-pt/strings.xml`.

`ChannelInfoOverlay` (overlay que abre ao tocar/pressionar INFO em Live TV):

- [ ] Removidos do `LazyRow` de quick actions: `Subs` (R.string.player_subs), `Audio` (R.string.player_audio), `Split Screen/Multiview` (R.string.player_multiview_short / R.string.player_action_split), `REC` (botÃ£o com `icon="REC"`), `C-UP` (botÃ£o com `icon="C-UP"` + `R.string.player_catchup_badge`) e `PiP` (R.string.player_pip_short).
- [ ] Removidos os painÃ©is `ChannelInfoPanel.RECORD` e `ChannelInfoPanel.CATCH_UP` (nÃ£o hÃ¡ mais botÃ£o que os acione). O enum `ChannelInfoPanel` agora sÃ³ tem `LIVE_DVR`.
- [ ] Removidos `recordButtonFocusRequester`, `recordPanelFocusRequester`, `catchUpButtonFocusRequester`, `catchUpPanelFocusRequester` e suas referÃªncias no composable.
- [ ] Removidos os parÃ¢metros da assinatura: `currentRecordingStatus`, `onStartRecording`, `onStopRecording`, `onScheduleRecording`, `onScheduleDailyRecording`, `onScheduleWeeklyRecording`, `onRestartProgram`, `onOpenArchive`, `onOpenSplitScreen`, `subtitleTrackCount`, `liveTranslationAvailable`, `audioTrackCount`, `onOpenSubtitleTracks`, `onOpenAudioTracks`, `onEnterPictureInPicture`. Os pills de status `RecordingStatus.RECORDING`/`SCHEDULED` no header tambÃ©m saÃ­ram (nÃ£o hÃ¡ mais gravaÃ§Ã£o no app pelo IPTV player). Pill `R.string.player_catchup_badge` no header tambÃ©m removido (nÃ£o hÃ¡ mais catch-up).
- [ ] Call-site em `PlayerScreen.kt:1306` ajustado removendo os argumentos correspondentes.
- [ ] Removidos imports `archivePlaybackCapability`, `isArchivePlayable`, `RecordingStatus` (nÃ£o hÃ¡ mais referÃªncias no composable).

`PlayerLiveInfo` (Live branch do `PlayerBottomBar`, controles completos de Live TV):

- [ ] Removido `Picture in picture` da primary actions list.
- [ ] Removidos `Subs`, `Audio`, `Video Quality` e `Multiview/Split Screen` da secondary actions list. A secondary list agora contÃ©m apenas `Aspect Ratio` e (condicional) `A/V Sync`.
- [ ] Removidos da assinatura de `PlayerLiveInfo`: `subtitleTrackCount`, `liveTranslationAvailable`, `audioTrackCount`, `videoQualityCount`, `onOpenSubtitleTracks`, `onOpenAudioTracks`, `onOpenVideoTracks`, `onOpenSplitScreen`, `onEnterPictureInPicture`. Call-site em `PlayerBottomBar` ajustado.

Strings (`values-pt/strings.xml`):

- [ ] `player_subs` corrigido de `Subscritores` para `Legendas`. Em PT, `player_subs` significa legendas do vÃ­deo, nÃ£o "subscribers". As variantes EN permanecem como `Subs`.
- [ ] `player_cast` e `player_action_cast` ajustados de `TransmissÃ£o` (definido na Fase 4h) para `Chromecast` (nome de marca usado em todos os locales, igual a `R.string.cast_button_label`). MantÃ©m-se `player_stop_casting` = `Pare de transmitir` (PT) por consistÃªncia com a Fase 4h.
- [ ] EN `player_cast` permanece como `Cast` e `player_action_cast` permanece como `Cast` (o usuÃ¡rio pediu apenas para corrigir o termo PT).

ResÃ­duo intencional fora do escopo desta fase:

- `MainActivity.onUserLeaveHint()` (PiP automÃ¡tico quando o usuÃ¡rio sai do app durante a reproduÃ§Ã£o) continua ativo. RemovÃª-lo exige mudar o manifesto, `MainActivity` e `enterPlayerPictureInPictureModeIfEligible`.
- ViewModels/Actions para gravaÃ§Ã£o (`viewModel.startManualRecording()`, `viewModel.stopCurrentRecording()`, `viewModel.scheduleRecording()`, etc.) continuam existindo, mas nÃ£o sÃ£o mais expostos pela UI.

## Fase 4j â€” ChannelInfoOverlay sem Quality (Live TV player)

Arquivos principais:
- `app/src/main/java/app/ehtudo/iptv/ui/screens/player/overlay/PlayerChannelInfoOverlay.kt` â€” funÃ§Ã£o `ChannelInfoOverlay`.
- `app/src/main/java/app/ehtudo/iptv/ui/screens/player/PlayerScreen.kt` â€” call-site de `ChannelInfoOverlay`.

- [ ] Removido do `LazyRow` de quick actions do `ChannelInfoOverlay`: `Quality` (`R.string.player_quality_short` + `R.string.player_action_quality`, acionado por `videoQualityCount > 0`). O servidor Xtream Eh! IPTV nÃ£o fornece mÃºltiplas trilhas de vÃ­deo para Live TV.
- [ ] Removidos da assinatura do `ChannelInfoOverlay`: `videoQualityCount: Int = 0` e `onOpenVideoTracks: () -> Unit = {}` (eram os Ãºnicos consumidores do botÃ£o Quality).
- [ ] Call-site em `PlayerScreen.kt:1306` ajustado removendo os argumentos correspondentes (`videoQualityCount = availableVideoQualities.size` e `onOpenVideoTracks = { showTrackSelection = TrackType.VIDEO }`).
- [ ] **ResÃ­duo Live TV:** as funÃ§Ãµes do ViewModel `viewModel.selectVideoQuality(...)` e o caminho `showTrackSelection = TrackType.VIDEO` continuam existindo; o overlay completo `PlayerControlsOverlay` (acessado por INFO/MENU) ainda passa `videoTracks` para o motor do player â€” apenas o botÃ£o da `ChannelInfoOverlay` foi removido.
- [ ] Build `:app:compileDebugKotlin` passa.

## Fase 4k â€” Identidade visual: regenerar banner e welcome com a arte EH! IPTV

Skill de referÃªncia: [`customise-banners-and-launcher-art.md`](./customise-banners-and-launcher-art.md) (#13). Complementa a Fase 1 (identidade e pacote) e deve ser reaplicado sempre que a arte canÃ´nica mudar.

Arquivos a regenerar:

- [ ] `app/src/main/res/drawable-*/ic_launcher_vault_art.png` (5 densities: mdpi 108, hdpi 162, xhdpi 216, xxhdpi 324, xxxhdpi 432). Ã‰ a **fonte canÃ´nica** â€” atualize aqui primeiro, o resto flui dela.
- [ ] `app/src/main/res/mipmap-*/ic_launcher_vault.png` (5 densities). Legado para pre-API 26 e alguns launchers. Roda o script Python da skill #13.
- [ ] `app/src/main/res/drawable-*/app_banner.png` (5 densities). Banner do launcher da Android TV. Roda o mesmo script â€” o script detecta `app_banner.png` e `ic_launcher_vault.png` automaticamente.
- [ ] `app/src/main/res/drawable/welcome_bg.png` (1536Ã—1024, arquivo Ãºnico). Use `image_synthesize` com a arte como referÃªncia e o prompt da skill #13; redimensione o output para 1536Ã—1024 antes de salvar.
- [ ] `mipmap-anydpi-v26/ic_launcher_vault.xml` e `drawable/ic_launcher_foreground.xml` **nÃ£o mudam** â€” a arte Ã© referenciada por `@drawable/ic_launcher_vault_art`, que resolve via density qualifier.

ValidaÃ§Ãµes de imagem (use Python com PIL):

- [ ] Os 5 `mipmap-*/ic_launcher_vault.png` sÃ£o o Ã­cone EH! IPTV (TV + 2 pessoas + texto "EH! IPTV") em laranja sobre fundo dark.
- [ ] Os 5 `drawable-*/app_banner.png` mostram o Ã­cone **inteiro** (sem corte nas bordas do TV nem no texto "EH! IPTV" â€” bug fÃ¡cil: se usar cover-crop em vez de contain, o texto vira "HI IPTV").
- [ ] Aspect ratio preservado: banners em 16:9, mipmaps em 1:1.

Build + verificar:

- [ ] `./gradlew :app:assembleDebug --no-daemon` passa.
- [ ] O `app/build/intermediates/packaged_res/.../app_banner.png` e os `mipmap-*/ic_launcher_vault.png` regenerados a partir do source (verifique com `ls -la` ou `Get-ChildItem` no diretÃ³rio de build/intermediates).
- [ ] ApÃ³s `adb install -r`, o launcher da TV mostra o banner novo (sem cache antigo â€” veja o `force-stop com.google.android.tvlauncher` da skill #12 se necessÃ¡rio).
- [ ] ApÃ³s `adb install -r`, o Ã­cone do app no drawer da TV/celular mostra a arte nova.

## Fase 5 â€” AtivaÃ§Ã£o e sincronizaÃ§Ã£o

Arquivo principal: `data/src/main/java/app/ehtudo/data/repository/ProviderRepositoryImpl.kt`.

- [ ] Login Xtream salva o provedor como `isActive = true` e `status = ACTIVE`.
- [ ] O caminho de ediÃ§Ã£o tambÃ©m mantÃ©m `isActive = true` e `status = ACTIVE`.
- [ ] ApÃ³s salvar, agenda a retomada da sincronizaÃ§Ã£o e o EPG em background.
- [ ] O login retorna apÃ³s despachar o trabalho, sem esperar o catÃ¡logo inteiro.
- [ ] `handleInitialOnboardingSync` continua preservado para M3U, Jellyfin e Stalker.

## Fase 6 â€” Defaults da experiÃªncia Eh!IPTV

Em instalaÃ§Ã£o limpa, confirme os defaults abaixo. O usuÃ¡rio ainda pode alterÃ¡-los nas configuraÃ§Ãµes depois.

- [ ] NavegaÃ§Ã£o superior padrÃ£o = `[SEARCH, LIVE_TV, MOVIES, SERIES, SETTINGS]`.
- [ ] Tela inicial padrÃ£o = `LIVE_TV`.
- [ ] `liveTvChannelMode` padrÃ£o = `PRO`.
- [ ] `liveTvQuickFilterVisibility` padrÃ£o = `HIDE`.
- [ ] Primeira categoria Live TV = `All Channels` (`ChannelRepository.ALL_CHANNELS_ID`).
- [ ] A Ã¡rea de TV ao vivo nÃ£o mostra filtros rÃ¡pidos no primeiro acesso.

Verifique em instalaÃ§Ã£o limpa:

```bash
adb -s d1d1b8f3 shell pm clear app.ehtudo.iptv.debug
```

## Fase 7 â€” Strings e marca

- [ ] `welcome_brand_title` = `Eh! IPTV`.
- [ ] `welcome_username_hint` = `UsuÃ¡rio`.
- [ ] `welcome_password_hint` = `Senha`.
- [ ] `welcome_save` = `Salvar`.
- [ ] `welcome_username_required` e `welcome_password_required` existem.
- [ ] `settings_providers` = `Eh!IPTV` em `values/strings.xml` e `values-pt/strings.xml`.
- [ ] O texto do botÃ£o Salvar Ã© branco no Welcome e em ConfiguraÃ§Ãµes.
- [ ] O fundo dos dois botÃµes Salvar Ã© azul-claro, preferencialmente `AppColors.BrandStrong`.
- [ ] Ao adicionar novos recursos, atualize os arquivos de traduÃ§Ã£o necessÃ¡rios.

## Fase 8 â€” Build e instalaÃ§Ã£o

```bash
./gradlew :app:assembleDebug --no-daemon
adb devices
adb -s d1d1b8f3 install -r app/build/outputs/apk/debug/app-debug.apk
```

- [ ] Build termina com `BUILD SUCCESSFUL`.
- [ ] O APK instala no Xiaomi `d1d1b8f3` sem `INSTALL_FAILED_USER_RESTRICTED`.
- [ ] Use `adb -s` explicitamente se houver mais de um dispositivo conectado.
- [ ] A instalaÃ§Ã£o incremental preserva os dados; use `pm clear` somente quando precisar validar uma instalaÃ§Ã£o limpa.

## Fase 9 â€” VerificaÃ§Ã£o funcional

1. Instale/limpe o app e abra o Welcome.
2. Confirme o tÃ­tulo, os dois campos e o botÃ£o Salvar azul-claro com texto branco.
3. Toque em Salvar vazio e confirme `Digite seu usuÃ¡rio`.
4. Informe credenciais vÃ¡lidas e confirme a entrada no app.
5. Abra ConfiguraÃ§Ãµes e confirme que a categoria se chama `Eh!IPTV`.
6. Exclua o Ãºltimo provedor, se necessÃ¡rio, e confirme que o formulÃ¡rio inline reaparece.
7. Confirme que Combined M3U e Adicionar provedor continuam ocultos.
8. Salve credenciais pelo formulÃ¡rio de ConfiguraÃ§Ãµes e confirme que o provedor aparece.
8.1. Abra a categoria `ReproduÃ§Ã£o` e confirme que ela mostra apenas `Live stream format` e o card de teste de velocidade.
8.2. Confirme que o rail lateral sÃ³ lista `Eh!IPTV`, `ReproduÃ§Ã£o`, `Privacidade` e `Sobre`. As categorias de NavegaÃ§Ã£o, GravaÃ§Ã£o, Backup e EPG sources nÃ£o devem aparecer.
8.3. Abra a categoria `Privacidade` e confirme que ela mostra apenas o toggle `ConteÃºdo adulto` e o item `Limpar histÃ³rico`. NÃ£o deve haver nenhuma linha de configuraÃ§Ã£o manual de PIN.
8.4. Com o toggle desligado, confirme que o status lateral Ã© `OCULTO` (nÃ­vel 3, conteÃºdo adulto nÃ£o aparece em nenhuma lista). Ligue o toggle e confirme que o status passa para `BLOQUEADO` (nÃ­vel 1, conteÃºdo adulto aparece mas exige PIN). O literal `0000` jamais aparece em tela.
8.5. Com o toggle ligado (BLOQUEADO), abra a lista de Live TV e confirme que as categorias adultas aparecem. Toque em uma delas e confirme que o app exige o PIN configurado antes de abrir o conteÃºdo.
8.6. Ligue o toggle pela primeira vez apÃ³s `pm clear` e verifique via `adb shell run-as` que `hasParentalPin` ficou `true`. Tente acessar uma categoria adulta no app e confirme que ela sÃ³ abre apÃ³s digitar o PIN.
8.7. Abra o diÃ¡logo `Limpar histÃ³rico` e confirme que ele dispara `viewModel.clearHistory()`.
8.8. Abra a categoria `Sobre` e confirme que ela mostra apenas `VersÃ£o`, `Site` e `Agradecimento`. Toque em `Site` e em `Agradecimento` e confirme que cada um abre o URL correspondente no navegador/handler padrÃ£o.
9. Verifique o banco apÃ³s o login:

```bash
adb -s d1d1b8f3 exec-out run-as app.ehtudo.iptv.debug cat databases/streamvault.db > /tmp/db.sqlite
sqlite3 /tmp/db.sqlite "SELECT id, name, is_active, status, server_url, username FROM providers;"
```

Esperado: nome `Eh! IPTV`, URL `http://dnstv.top/`, `is_active = 1` e `status = ACTIVE`.

10. Aguarde a sincronizaÃ§Ã£o e confirme canais Live TV:

```bash
adb -s d1d1b8f3 logcat -d -t 300 | grep -iE "ProviderSync|XtreamIndex|BackgroundEpg"
sqlite3 /tmp/db.sqlite "SELECT COUNT(*) FROM channels;"
```

## Fase 10 â€” AtualizaÃ§Ã£o do roteiro apÃ³s novas mudanÃ§as

Ao reaplicar o roteiro sobre uma nova base StreamVault:

1. Rode `git log --follow -- docs/skill/iptv-reseller-simplification-checklist.md`.
2. Compare os commits posteriores ao Ãºltimo commit que alterou este arquivo.
3. Inspecione `git show <commit>` e incorpore ao roteiro somente mudanÃ§as de produto, comandos de build, caminhos e verificaÃ§Ãµes que ainda sejam vÃ¡lidos.
4. Atualize o campo de referÃªncia da versÃ£o/commit no inÃ­cio deste documento.
5. Rode `git diff --check` e revise o diff completo.
6. Rode `graphify update .` apÃ³s modificar cÃ³digo; se o comando nÃ£o existir no ambiente, registre a limitaÃ§Ã£o.

## Fase 11 â€” Desabilitar TV Input Service (instalar como phone)

Skill de referÃªncia: [`disable-tv-input-service.md`](./disable-tv-input-service.md) (#12). Aplica quando o servidor do revendedor nÃ£o expÃµe um feed de TV input real (a maioria dos servidores Xtream) e o "StreamVault Live Channels" aparecendo no menu Inputs/Sources da TV confunde o usuÃ¡rio (abre um modal separado que leva Ã  tela de escolha de fonte, divergente do welcome flow).

Arquivos a deletar:

- [ ] `app/src/main/java/app/ehtudo/iptv/tvinput/StreamVaultTvInputService.kt`
- [ ] `app/src/main/java/app/ehtudo/iptv/tvinput/TvInputSetupActivity.kt`
- [ ] `app/src/main/java/app/ehtudo/iptv/tvinput/TvInputChannelSyncManager.kt`
- [ ] `app/src/main/res/xml/tv_input_service.xml`

`AndroidManifest.xml` â€” remover trÃªs blocos:

- [ ] 3 permissÃµes TV: `BIND_TV_INPUT`, `READ_EPG_DATA`, `WRITE_EPG_DATA` (no topo do manifest).
- [ ] O `<activity android:name=".tvinput.TvInputSetupActivity" ... />`.
- [ ] O `<service android:name=".tvinput.StreamVaultTvInputService" ... />` (com `android:label="StreamVault Live Channels"` e o meta-data `@xml/tv_input_service`).

CÃ³digo Kotlin â€” remover de 6 arquivos (import + `@Inject` field + call sites + argumento no construtor):

- [ ] `app/src/main/java/app/ehtudo/iptv/MainActivity.kt`
- [ ] `app/src/main/java/app/ehtudo/iptv/plugins/StreamVaultPluginManager.kt`
- [ ] `app/src/main/java/app/ehtudo/iptv/ui/screens/settings/SettingsViewModel.kt`
- [ ] `app/src/main/java/app/ehtudo/iptv/ui/screens/settings/SettingsSyncActions.kt` (tambÃ©m remover o `if (completed.any { it == ... settings_sync_option_tv ... }) { tvInputChannelSyncManager.refreshTvInputCatalog() }` â€” fica dead code)
- [ ] `app/src/main/java/app/ehtudo/iptv/ui/screens/settings/SettingsProviderActions.kt`
- [ ] `app/src/main/java/app/ehtudo/iptv/ui/screens/home/HomeViewModel.kt`

Testes (limpar antes do prÃ³ximo `testDebugUnitTest`):

- [ ] Deletar `app/src/test/java/app/ehtudo/iptv/tvinput/TvInputChannelSyncManagerTest.kt`.
- [ ] Limpar referÃªncias em `app/src/test/java/app/ehtudo/iptv/ui/screens/settings/SettingsProviderActionsTest.kt`.
- [ ] Limpar referÃªncias em `app/src/test/java/app/ehtudo/iptv/ui/screens/home/HomeViewModelTest.kt`.

Strings (opcional, em `app/src/main/res/values/strings.xml`):

- [ ] Remover as 15 strings com prefixo `tv_input_setup_*` (deixei nos 27 locales como inerte â€” cleanup separado).

ValidaÃ§Ãµes:

- [ ] `rg "tvinput|TvInput|TV_INPUT|READ_EPG_DATA|WRITE_EPG_DATA|BIND_TV_INPUT" app/src/main` retorna vazio (exceto locale files se deferiu cleanup).
- [ ] `./gradlew :app:assembleDebug --no-daemon` passa.
- [ ] ApÃ³s `adb uninstall` + `adb install` na TV, abrir o menu `Inputs / Sources` da TV. A entrada "Eh! IPTV" / "StreamVault Live Channels" **nÃ£o** aparece.
- [ ] Abrir o app pelo launcher da TV â€” entra direto no welcome (ou dashboard, se jÃ¡ configurado), **sem** modal "select your provider".

NÃ£o fazer:

- NÃ£o ocultar o `<service>` por flag de build. O sistema de TV indexa a entrada agressivamente; o Ãºnico jeito de remover Ã© deletar o cÃ³digo + `uninstall` antes do prÃ³ximo `install`.
- NÃ£o remover `LEANBACK_LAUNCHER` do intent-filter do MainActivity junto com esta fase. O app continua pertencendo ao launcher da TV â€” sÃ³ nÃ£o se registra como input.
- NÃ£o reaproveitar `StreamVaultTvInputService.kt` como stub vazio "pra reativar depois". O cÃ³digo puxa `androidx.tvprovider`, `TvContract` e parte de `media.tv` que apodrecem.

## RegressÃµes conhecidas

| Sintoma | VerificaÃ§Ã£o inicial |
|---|---|
| Salvar nÃ£o faz nada | Confirme o `onClick` do `TvButton` e a referÃªncia do ViewModel. |
| Erro nÃ£o aparece | Confirme `quickXtreamError`/`error` e as strings de validaÃ§Ã£o. |
| Provedor nÃ£o aparece em ConfiguraÃ§Ãµes | Verifique o observer de `providerRepository.getProviders()`. |
| Provedor volta inativo | Consulte `is_active` e `status` no banco e revise o caminho Xtream. |
| SincronizaÃ§Ã£o nÃ£o inicia | Verifique logs de `ProviderSync`, `XtreamIndex` e `BackgroundEpg`. |
| Combined M3U reaparece | Procure `CombinedM3uProfilesCard` em `SettingsProviderSection.kt`. |
| BotÃ£o Salvar perde contraste | Confirme `ButtonDefaults.colors`, `AppColors.BrandStrong` e `Color.White` nos dois cards. |
| Xiaomi recusa instalaÃ§Ã£o | Habilite `Instalar via USB` nas opÃ§Ãµes do desenvolvedor do aparelho. |
| ReproduÃ§Ã£o mostra toggles extras | Verifique `SettingsPlaybackSection.kt`; apenas `Live stream format` e `InternetSpeedTestCard` devem ser emitidos. |
| Rail mostra categorias ocultas | Remova as entradas de `SettingsNavigationRail.kt` e ajuste o `if/else` em `SettingsContentPane.kt` para os Ã­ndices restantes. |
| Privacidade mostra ParentalControlCard ou toggles de IncÃ³gnito/Xtream | Reescreva `SettingsPrivacySection.kt` para emitir apenas `AdultContentToggleRow` (Compose local) e o card `Limpar histÃ³rico`; apague as chamadas a `toggleIncognitoMode`, `toggleXtreamTextClassification` e `toggleXtreamBase64TextCompatibility`. |
| PIN padrÃ£o nÃ£o Ã© definido ao ligar o toggle | Garanta que `setAdultContentEnabled(true)` chama `preferencesRepository.setParentalPin("0000")` na primeira vez e ajusta `parentalControlLevel` para `3`. |
| Toggle e status visual nÃ£o sincronizam | Verifique `SettingsPreferenceSnapshotMapper.kt`: `adultContentEnabled` deve ser `parentalControlLevel == 1 || == 2` (LOCKED/PRIVATE). |
| Status lateral mostra `OCULTO`/`BLOQUEADO` invertido | Confirme `settings_adult_content_status_hidden` (= `OCULTO`) e `settings_adult_content_status_locked` (= `BLOQUEADO`) em `strings.xml`/`values-pt`, e que `AdultContentToggleRow` lÃª `locked` quando o toggle estÃ¡ ligado. |
| ConteÃºdo adulto nÃ£o exige PIN quando BLOQUEADO | Confirme `setAdultContentEnabled(true)` chama `setParentalControlLevel(PARENTAL_LEVEL_LOCKED)` (1). O fluxo de PIN em listas/categorias deve continuar exigindo o PIN jÃ¡ configurado. |
| Aparece item "Configurar PIN" na Privacidade | NÃ£o deve haver nenhum item para alterar o PIN do conteÃºdo adulto. Remova `AdultContentConfigurePinRow` e os callbacks `onShowPinDialogChange`/`onPendingActionChange` da assinatura de `settingsPrivacySection(...)`. |
| Literal `0000` aparece na tela de Privacidade | A `subtitle` do toggle nÃ£o pode mencionar o PIN. Remova qualquer referÃªncia a `0000` da `settings_adult_content_subtitle`. |
| Sobre mostra AtualizaÃ§Ãµes/Crash Reports/Build Info | Reescreva `settingsAboutSection(...)` para emitir apenas `SettingsRow(version)` + `ClickableSettingsRow(site)` + `ClickableSettingsRow(acknowledgment)`. Apague os blocos antigos de Updates e Crash Reports. |
| Hero strip ou linha de pÃ­lulas reaparecem no topo de Filmes/SÃ©ries | Verifique `MoviesScreen.kt` e `SeriesScreen.kt`: os blocos `item(key = "hero") { ... }` e `item(key = "actions") { ... }` na `LazyColumn` do modo preview devem estar ausentes. O `focusRequester(initialFocusRequester)` deve estar no primeiro card de `favorites_row`/`fresh_row`/`top_rated_row`/`catEntries`, nÃ£o em um `VodHeroStrip`. |
| Foco nÃ£o cai em nenhum card ao entrar em Filmes/SÃ©ries | Confirme que `Modifier.focusRequester(initialFocusRequester)` estÃ¡ sendo anexado ao primeiro card de `favorites_row` via `.then(if (movie.id == fallbackMovieId) Modifier.focusRequester(initialFocusRequester) else Modifier)` e que `fallbackMovieId` nÃ£o estÃ¡ `null`. Garanta que `FocusRestoreHost` ainda chama `initialContentFocusRequester.requestFocusSafely(...)`. |
| BotÃµes `Copy URL` ou `Download` reaparecem nos detalhes VOD | Verifique `MovieDetailScreen.kt` (aÃ§Ã£o do filme), `SeriesDetailActions` (aÃ§Ã£o da sÃ©rie) e `EpisodeItem` (episÃ³dios). Em nenhum dos trÃªs deve haver `TvButton` lendo `R.string.stream_url_copy` ou `R.string.download_button_label`. A linha da sÃ©rie deve manter `Play`/`Resume Â· â€¦`, `Chromecast` e o coraÃ§Ã£o; cada episÃ³dio mantÃ©m apenas `Chromecast`. |
| Detalhes do filme nÃ£o focam no botÃ£o Play | Confirme que `MovieDetailHeroText` continua invocando `TvButton(onClick = onPlay, modifier = Modifier.focusRequester(playButtonFocusRequester), â€¦)` e que `LaunchedEffect(movie.id) { playButtonFocusRequester.requestFocusSafely(...) }` permanece em `MovieDetailContent`. |
| BotÃµes `Qualidade`, `Ãudio`, `Parar reproduÃ§Ã£o`, `Standby` ou `PiP` reaparecem no player VOD | Verifique `PlayerControlsChrome.kt:PlayerVodInfo` (a action list comeÃ§a em `PlayerActionsChrom.kt:1063` apÃ³s a Fase 4h). Nenhum `add(PlayerActionSpec(...))` deve referenciar `R.string.player_video_quality`, `R.string.player_audio`, `R.string.player_stop_playback_after`, `R.string.player_idle_standby_after` ou `R.string.player_picture_in_picture`. Live TV ainda tem esses botÃµes â€” Ã© intencional fora do escopo da Fase 4h. |
| BotÃ£o Cast mostra `Elenco` em portuguÃªs | Confirme `values-pt/strings.xml:805` (`player_action_cast` = `Chromecast`) e `:810` (`player_cast` = `Chromecast`). Se algum outro locale (`values-es`, etc.) ainda diz `Elenco`, foi deixado como estÃ¡ nesta fase. |
| BotÃ£o `Subs` mostra `Subscritores` em portuguÃªs | Confirme `values-pt/strings.xml:802` (`player_subs` = `Legendas`). `Elenco` e `Subscritores` foram traduzidos errados na Fase 4h e na Fase 4i respectivamente. |
| BotÃµes `Subs`, `Audio`, `Rec`, `C-UP`, `Split` ou `PiP` reaparecem no Channel Info Overlay | Verifique `PlayerChannelInfoOverlay.kt` (LazyRow em torno da linha 380). Nenhum `QuickActionButton` deve referenciar `R.string.player_subs`, `R.string.player_audio`, `R.string.player_multiview_short`, `R.string.player_catchup_badge`, `R.string.player_pip_short` nem usar `icon="REC"` ou `icon="C-UP"`. O enum `ChannelInfoPanel` deve ter apenas `LIVE_DVR`. |
| BotÃµes `Subs`, `Audio`, `Split` ou `PiP` reaparecem no overlay Live completo | Verifique `PlayerLiveInfo` em `PlayerControlsChrome.kt`: primary actions nÃ£o devem ter `Picture in picture`; secondary actions nÃ£o devem ter `Subs`, `Audio`, `Video Quality` nem `Multiview`. |
| BotÃ£o `Quality` reaparece no Channel Info Overlay | Verifique `PlayerChannelInfoOverlay.kt` LazyRow: nÃ£o deve haver `QuickActionButton` com `R.string.player_quality_short` nem `R.string.player_action_quality`. Os parÃ¢metros `videoQualityCount` e `onOpenVideoTracks` tambÃ©m devem estar ausentes da assinatura de `ChannelInfoOverlay`. |

## NÃ£o fazer

- NÃ£o adicionar URL editÃ¡vel ao fluxo simplificado.
- NÃ£o mascarar a senha se a especificaÃ§Ã£o exigir texto simples.
- NÃ£o adicionar o botÃ£o Adicionar provedor novamente Ã  seÃ§Ã£o simplificada sem revisar este roteiro.
- NÃ£o reintroduzir Combined M3U na tela de ConfiguraÃ§Ãµes sem decisÃ£o explÃ­cita do produto.
- NÃ£o reintroduzir categorias do rail (NavegaÃ§Ã£o, GravaÃ§Ã£o, Backup, EPG sources) sem revisar este roteiro.
- NÃ£o reintroduzir toggles extras de ReproduÃ§Ã£o alÃ©m de `Live stream format` e teste de velocidade sem revisar este roteiro.
- NÃ£o reintroduzir o `ParentalControlCard` nem os toggles de IncÃ³gnito / Xtream name-based adult detection / Xtream Base64 na seÃ§Ã£o Privacidade sem revisar este roteiro.
- NÃ£o reintroduzir AtualizaÃ§Ãµes automÃ¡ticas, Crash Reports, Build info, GitHub ou DoaÃ§Ãµes na seÃ§Ã£o Sobre sem revisar este roteiro.
- NÃ£o reintroduzir o `hero` strip (`VodHeroStrip`) nem a linha de pÃ­lulas (`VodActionChipRow` com `browse_all`/`categories`/`favorites`/`resume`/`top_rated`/`fresh`) no topo das pÃ¡ginas de Filmes e SÃ©ries (modo preview) sem revisar este roteiro.
- NÃ£o reintroduzir `Copy URL` ou `Download` nas linhas de aÃ§Ã£o de `MovieDetailScreen`, `SeriesDetailActions` (aÃ§Ã£o da sÃ©rie) ou `EpisodeItem` sem revisar este roteiro. Cada aÃ§Ã£o da sÃ©rie/detalhe do filme deve manter apenas Play/Resume, Chromecast e Favorito (e Trailer no filme quando houver); cada episÃ³dio mantÃ©m apenas Chromecast.
- NÃ£o reintroduzir `Qualidade do vÃ­deo`, `Ãudio`, `Parar reproduÃ§Ã£o apÃ³s`, `Permitir standby apÃ³s inatividade` ou `Imagem em imagem` no player VOD (`PlayerVodInfo` em `PlayerControlsChrome.kt`) sem revisar este roteiro. Live TV nÃ£o foi tocado nesta fase â€” para remover tambÃ©m em Live, abrir nova fase.
- NÃ£o reintroduzir o termo `Elenco` para o botÃ£o de Cast/Chromecast em `values-pt/strings.xml` â€” o termo correto Ã© `Chromecast` (literal, como `cast_button_label`). Ver `player_cast` e `player_action_cast`.
- NÃ£o reintroduzir `Subscritores` em `player_subs` em `values-pt/strings.xml` â€” o termo correto Ã© `Legendas`.
- NÃ£o reintroduzir os botÃµes `Subs`, `Audio`, `Rec`, `C-UP`, `Split Screen` ou `PiP` no `ChannelInfoOverlay`/`PlayerLiveInfo` (Fase 4i) sem revisar este roteiro. O enum `ChannelInfoPanel` deve continuar com apenas `LIVE_DVR`.
- NÃ£o remover a autenticaÃ§Ã£o do `ValidateAndAddProvider`.
- NÃ£o bloquear o Welcome aguardando o catÃ¡logo inteiro.
- NÃ£o commitar credenciais de cliente.
- NÃ£o misturar renome de pacote com mudanÃ§as visuais e de onboarding no mesmo commit quando uma separaÃ§Ã£o for possÃ­vel.
- NÃ£o reintroduzir o `StreamVaultTvInputService` (ou o label "StreamVault Live Channels" no menu Inputs da TV) sem antes confirmar que o servidor do revendedor expÃµe um feed TV input real. A Fase 11 remove a feature por padrÃ£o.
- NÃ£o usar cover-crop ao regenerar `app_banner.png` (Fase 4k) â€” o Ã­cone tem o texto "EH! IPTV" nas bordas e cover-crop corta em "HI IPTV". Usar `contain` (fit inside, sem crop) com 8% de padding.
