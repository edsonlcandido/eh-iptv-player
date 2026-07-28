# Roteiro — simplificação do StreamVault para Eh!IPTV

## Objetivo

Usar este documento como checklist repetível para transformar uma nova cópia do projeto base **StreamVault** em uma build simplificada de revenda chamada **Eh!IPTV**. Execute as fases na ordem, confirme cada item e só marque a fase como concluída após a verificação correspondente.

Este roteiro consolida as alterações implementadas até o commit atual (`57da4be simplificado configurações`, branch `ehiptv/custom-and-simplify`), com Reprodução enxuta, categorias ocultas no rail, a seção Privacidade reduzida ao toggle de Conteúdo adulto + Limpar histórico, a seção Sobre limitada a Versão/Site/Agradecimento, a Fase 4f (VOD enxuto em Filmes e Séries), a Fase 4g (Detalhes VOD enxutos — somente Play/Chromecast/Favorito/Trailer), a Fase 4h (Player VOD enxuto — sem Quality/Audio/Stop Playback/Standby/PiP, e `Elenco` → `Transmissão` em PT), a Fase 4i (Player IPTV enxuto — sem Subs/Audio/Rec/C-UP/Split/PiP; `Subscritores` → `Legendas`; `Transmissão` → `Chromecast` em PT) e a Fase 4j (ChannelInfoOverlay sem Quality no Live TV). A versão anterior estava alinhada até `817d3c9` (`Campo provedores alterado para Eh! IPTV`).

## Quando usar

- Ao iniciar a customização de uma nova versão/clone do StreamVault.
- Ao reaplicar a personalização depois de atualizar o projeto base.
- Ao revisar uma build antes de entregar o APK ao revendedor.

## Fase 0 — Preparar o projeto base

1. Crie uma branch de customização a partir do StreamVault:

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
5. Depois de renomear pacotes, limpe os caches KSP:

```bash
rm -rf app/build/kspCaches data/build/kspCaches player/build/kspCaches domain/build/kspCaches
```

## Fase 1 — Identidade e pacote

- [ ] Pacote da aplicação = `app.ehtudo.iptv`.
- [ ] Build debug adiciona o sufixo `.debug`.
- [ ] Marca exibida ao usuário = `Eh! IPTV` ou `Eh!IPTV`, conforme o contexto visual.
- [ ] Título do app e ícones foram atualizados sem alterar o contrato de módulos.
- [ ] O nome do projeto Gradle pode continuar `StreamVault`; ele não precisa ser renomeado para funcionar.
- [ ] Confirme `applicationId` e `versionName` em `app/build.gradle.kts`.

## Fase 2 — Welcome simplificado

Arquivos principais: `app/src/main/java/app/ehtudo/iptv/ui/screens/welcome/WelcomeScreen.kt` e `app/src/main/res/values/strings.xml`.

- [ ] Título = `R.string.welcome_brand_title` (`Eh! IPTV`).
- [ ] A tela inicial mostra somente dois campos: usuário e senha.
- [ ] Os dois campos são texto simples: sem `PasswordVisualTransformation`, ícone de olho ou `KeyboardType.Password`.
- [ ] O usuário e a senha são validados antes da chamada de login.
- [ ] O botão usa `R.string.welcome_save` (`Salvar`).
- [ ] O botão Salvar usa fundo azul-claro (`AppColors.BrandStrong`) e texto branco.
- [ ] O login constrói `XtreamProviderSetupCommand` com:
  - `serverUrl = "http://dnstv.top/"`;
  - `name = "Eh! IPTV"`;
  - `xtreamFastSyncEnabled = true`.
- [ ] O login não bloqueia a entrada aguardando a sincronização completa.
- [ ] A sincronização posterior ocorre em background.

## Fase 3 — Provider Setup avançado

Arquivo principal: `app/src/main/java/app/ehtudo/iptv/ui/screens/provider/ProviderSetupScreen.kt`.

- [ ] O fluxo Xtream simplificado pede apenas usuário e senha.
- [ ] A URL Xtream padrão e o nome padrão são aplicados automaticamente.
- [ ] Não há campo de servidor, nome de playlist ou opções avançadas no fluxo simplificado.
- [ ] M3U, Stalker e Jellyfin continuam acessíveis pelo setup avançado.
- [ ] O fluxo avançado não perde os campos específicos de cada tipo.

## Fase 4 — Configurações > Eh!IPTV

Arquivos principais:
- `app/src/main/java/app/ehtudo/iptv/ui/screens/settings/SettingsNavigationRail.kt`
- `app/src/main/java/app/ehtudo/iptv/ui/screens/settings/SettingsProviderSection.kt`
- `app/src/main/java/app/ehtudo/iptv/ui/screens/settings/SettingsViewModel.kt`
- `app/src/main/res/values/strings.xml`

- [ ] A categoria lateral que antes era `Providers/Provedores` exibe `Eh!IPTV` através de `R.string.settings_providers`.
- [ ] Quando já existem provedores, a tela mostra a seleção e o card de gerenciamento do provedor.
- [ ] A seção `Combined M3U` não é exibida nessa tela.
- [ ] O botão `Adicionar provedor` não é exibido nessa tela.
- [ ] Quando não existe nenhum provedor, a tela mostra inline:
  - campo de usuário;
  - campo de senha em texto simples;
  - botão `Salvar`;
  - mensagem de erro de validação ou autenticação.
- [ ] O formulário vazio usa o mesmo URL Xtream fixo `http://dnstv.top/` e nome padrão `Eh! IPTV` do Welcome.
- [ ] O botão Salvar do formulário de provedores usa fundo azul-claro (`AppColors.BrandStrong`) e texto branco.
- [ ] O formulário fica desabilitado durante a criação/sincronização inicial.
- [ ] Editar, excluir, conectar, atualizar e controle parental continuam disponíveis para provedores existentes.

## Fase 4b — Reprodução enxuta

Arquivos principais:
- `app/src/main/java/app/ehtudo/iptv/ui/screens/settings/SettingsPlaybackSection.kt`
- `app/src/main/java/app/ehtudo/iptv/ui/screens/settings/SettingsContentPane.kt`
- `app/src/main/res/values/strings.xml`

- [ ] A categoria **Reprodução/Playback** do rail só exibe duas coisas: a linha `Live stream format` e o card de teste de velocidade (`InternetSpeedTestCard`).
- [ ] Nenhum toggle extra de decoder, timeshift, legendas, buffer, qualidade de rede, modo zap, compatibilidade, sincronização, Multiview ou sessão multimídia é renderizado.
- [ ] A linha `Live stream format` continua abrindo o diálogo `PremiumSelectionDialog` com `AUTO`, `HLS` e `MPEG_TS`, persistindo em `viewModel.setPlayerLiveStreamFormatMode(...)`.
- [ ] O card de teste de velocidade continua mostrando o último resultado, o botão **Rodar teste**, e os botões **Aplicar ao Wi-Fi** e **Aplicar ao cabo**, chamando `viewModel::runInternetSpeedTest`, `viewModel::applySpeedTestRecommendationToWifi` e `viewModel::applySpeedTestRecommendationToEthernet`.
- [ ] A assinatura de `settingsPlaybackSection(...)` foi enxugada para apenas `uiState`, `viewModel`, `lastSpeedTestLabel`, `lastSpeedTestSummary` e `speedTestRecommendationLabel`. Os demais labels e callbacks de diálogo podem ser removidos do call-site em `SettingsContentPane`.
- [ ] A string `settings_live_stream_format` existe em `values/strings.xml` e substitui o rótulo literal que existia dentro da seção.

## Fase 4c — Categorias ocultas no rail

Arquivo principal: `app/src/main/java/app/ehtudo/iptv/ui/screens/settings/SettingsNavigationRail.kt`.

- [ ] O rail lateral de Configurações mostra apenas as quatro categorias: `Eh!IPTV`, `Reprodução/Playback`, `Privacidade/Privacy` e `Sobre/About`.
- [ ] As categorias a seguir não são renderizadas em nenhum estado:
  - Navegação/Browsing.
  - Gravação/Recording.
  - Backup & Restore.
  - EPG Sources.
- [ ] O switch do `LazyColumn` em `SettingsContentPane.kt` consome apenas os índices `0..3`; ramos para índices maiores são removidos.
- [ ] `SettingsScreen.kt` não força mais `dialogState.selectedCategory = 5` no caminho de import inicial; a inspeção de backup segue acontecendo, mas sem selecionar uma categoria oculta.
- [ ] Strings das categorias ocultas (`settings_browsing`, `settings_recording_title`, `settings_backup_restore`, `EPG Sources`) podem permanecer em `strings.xml` para evitar impacto em outros consumidores.

## Fase 4d — Privacidade enxuta (somente Conteúdo adulto + Limpar histórico)

Arquivos principais:
- `app/src/main/java/app/ehtudo/iptv/ui/screens/settings/SettingsPrivacySection.kt`
- `app/src/main/java/app/ehtudo/iptv/ui/screens/settings/SettingsUiStateModel.kt`
- `app/src/main/java/app/ehtudo/iptv/ui/screens/settings/SettingsViewModel.kt`
- `app/src/main/java/app/ehtudo/iptv/ui/screens/settings/SettingsContentPane.kt`
- `app/src/main/java/app/ehtudo/iptv/ui/screens/settings/SettingsPreferenceSnapshotMapper.kt`
- `app/src/main/res/values/strings.xml` e `values-pt/strings.xml`

- [ ] A categoria `Privacidade` mostra apenas o toggle `Conteúdo adulto` e o card `Limpar histórico de visualização`. **Não há** item de configuração manual de PIN — o PIN padrão `0000` é fixo e gravado automaticamente na primeira ativação.
- [ ] O toggle `Conteúdo adulto` (chave `settings_adult_content`) controla um único `Switch` que altera `viewModel.setAdultContentEnabled(...)`.
- [ ] Quando o toggle está **desligado**, a área à direita do título mostra `OCULTO` (chave `settings_adult_content_status_hidden`) e o nível de proteção (`parentalControlLevel`) é `3` (HIDDEN) — o conteúdo adulto não aparece em nenhuma lista do app.
- [ ] Quando o toggle está **ligado**, a área à direita do título mostra `BLOQUEADO` (chave `settings_adult_content_status_locked`) e o nível de proteção é `1` (LOCKED) — o conteúdo adulto aparece nas listas, mas exige o PIN para abrir cada categoria.
- [ ] Na primeira ativação, o app grava o PIN padrão `0000` (constante `DEFAULT_ADULT_CONTENT_PIN` em `SettingsViewModel.kt`) e marca `hasParentalPin = true`. Ativações subsequentes preservam o PIN já configurado pelo usuário.
- [ ] O literal `0000` nunca é exibido na interface. O subtitle do toggle descreve apenas a função, sem mencionar o PIN.
- [ ] O item `Limpar histórico de visualização` continua abrindo o diálogo existente (`showClearHistoryDialog`) que chama `viewModel.clearHistory()`.
- [ ] Não devem ser renderizados em `settingsPrivacySection(...)`: `ParentalControlCard` (níveis OFF/LOCKED/PRIVATE/HIDDEN + alterar PIN), toggles de Incógnito, Xtream name-based adult detection e Xtream Base64 compatibility, e qualquer item de "Configurar PIN" / "Alterar PIN" para o PIN adulto.
- [ ] Strings e funções legadas (`settings_incognito_mode`, `settings_xtream_text_classification`, `settings_xtream_base64_compatibility`, `toggleIncognitoMode`, `toggleXtreamTextClassification`, `toggleXtreamBase64TextCompatibility`, `ParentalControlCard`, `ParentalAction`) podem permanecer no código desde que não sejam referenciadas pela seção de Privacidade. Outros consumidores não devem quebrar.
- [ ] `SettingsPreferenceSnapshotMapper.kt` define `adultContentEnabled = parentalControlLevel == 1 || parentalControlLevel == 2` (LOCKED ou PRIVATE — em ambos o conteúdo adulto aparece e exige PIN). Toggle OFF ↔ nível 3 (HIDDEN); toggle ON ↔ nível 1 (LOCKED).
- [ ] `SettingsViewModel.kt` define as constantes `PARENTAL_LEVEL_LOCKED = 1` e `PARENTAL_LEVEL_HIDDEN = 3` para que `setAdultContentEnabled(true)` mapeie para LOCKED e `setAdultContentEnabled(false)` mapeie para HIDDEN.
- [ ] A assinatura pública de `settingsPrivacySection(...)` foi enxugada para apenas `uiState`, `viewModel` e `onShowClearHistoryDialogChange`. Os callbacks `onShowPinDialogChange`/`onPendingActionChange` foram removidos do call-site em `SettingsContentPane`.
- [ ] Strings em `values/strings.xml`: `settings_adult_content`, `settings_adult_content_subtitle`, `settings_adult_content_status_hidden` (= `OCULTO`), `settings_adult_content_status_locked` (= `BLOQUEADO`). Em `values-pt/strings.xml`: as traduções equivalentes em português. As strings `settings_adult_content_status_configured`/`settings_adult_content_status_configure`/`settings_adult_content_configure_pin`/`settings_adult_content_configure_pin_subtitle` e qualquer menção visível a `0000` foram removidas.

## Fase 4e — Sobre enxuto (somente Versão, Site e Agradecimento)

Arquivos principais:
- `app/src/main/java/app/ehtudo/iptv/ui/screens/settings/SettingsBackupAboutSections.kt`
- `app/src/main/java/app/ehtudo/iptv/ui/screens/settings/SettingsContentPane.kt`
- `app/src/main/res/values/strings.xml` e `values-pt/strings.xml`

- [ ] A categoria `Sobre` mostra apenas três linhas: `Versão do aplicativo`, `Site` (clicável) e `Agradecimento` (clicável).
- [ ] `Versão do aplicativo` exibe `${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})`.
- [ ] `Site` mostra `https://iptv.ehtudo.app/` e abre o URL no `onOpenUri(...)`. Constante `EH_IPTV_SITE_URL` em `SettingsBackupAboutSections.kt`.
- [ ] `Agradecimento` mostra `https://github.com/Davidona/StreamVault-IPTV` e abre o URL. Constante `STREAMVAULT_REPO_URL` no mesmo arquivo.
- [ ] Seções removidas: Atualizações (auto-check, auto-download, latest release, status, last checked, check now, download, view release, error), Crash Reports e Build info (build, build verification, developed by, GitHub, donate).
- [ ] Assinatura de `settingsAboutSection(...)` foi enxugada para apenas `onOpenUri`. Os callbacks removidos ficam disponíveis na assinatura de `SettingsContentPane` caso outros fluxos queiram reaproveitar, mas nada os referencia após esta fase.
- [ ] Imports não usados em `SettingsBackupAboutSections.kt` (`LaunchedEffect`, `AppUpdateActionState`) foram removidos.
- [ ] Strings novas em `values/strings.xml`: `settings_site`, `settings_site_url`, `settings_acknowledgment`, `settings_acknowledgment_url`. Em `values-pt/strings.xml`: `settings_site`, `settings_site_url`, `settings_acknowledgment` (= `Agradecimento`), `settings_acknowledgment_url`.

## Fase 4f — VOD enxuto (somente prateleiras em Filmes e Séries)

Arquivos principais:
- `app/src/main/java/app/ehtudo/iptv/ui/screens/movies/MoviesScreen.kt`
- `app/src/main/java/app/ehtudo/iptv/ui/screens/series/SeriesScreen.kt`

- [ ] A página de **Filmes** mostra apenas as prateleiras (em ordem): Continuar assistindo → Favoritos → Mais recentes → Mais bem avaliados → Categorias.
- [ ] A página de **Séries** mostra apenas as prateleiras equivalentes (na mesma ordem, com títulos traduzidos via `library_lens_fresh_series`).
- [ ] O `hero` strip (`VodHeroStrip`) **não** é renderizado na `LazyColumn` do modo preview (default) — o bloco `item(key = "hero") { ... }` foi removido.
- [ ] A linha de pílulas (`VodActionChipRow` com `browse_all` / `categories` / `favorites` / `resume` / `top_rated` / `fresh`) **não** é renderizada no topo — o bloco `item(key = "actions") { ... }` foi removido.
- [ ] O `MoviesVodClassicContent` e o `SeriesVodClassicContent` (modo CLASSIC, `VodViewMode.CLASSIC`) **podem** continuar usando `VodActionChipRow` para os filtros de Browse/Filter — esses vivem abaixo do `VodClassicContentHeader` e são independentes do chrome superior.
- [ ] As variáveis `heroMovie`/`heroSeries` (derivadas de `freshMovies/favoriteMovies`) foram removidas das duas telas; não há mais fallback `if (hero == null)`.
- [ ] `fallbackMovieId`/`fallbackSeriesId` é derivado diretamente da primeira prateleira não-vazia (`favoriteMovies` → `freshMovies` → `topRatedMovies` → `catEntries`).
- [ ] O `initialFocusRequester` foi movido do hero para o primeiro card da primeira prateleira visível. Padrão adotado em `favorites_row`, `fresh_row`, `top_rated_row` e em `catEntries`: `Modifier.then(if (movie.id == fallbackMovieId) Modifier.focusRequester(initialFocusRequester) else Modifier)` (e a variante com `.width(favoriteCardWidth)` no `favorites_row`). `FocusRestoreHost` continua sendo acionado normalmente.
- [ ] O import `VodHeroStrip` foi removido de `MoviesScreen.kt` e `SeriesScreen.kt` (nenhum uso restante após a remoção do hero).
- [ ] Os imports `VodActionChipRow` e `VodActionChip` continuam presentes porque o modo CLASSIC e o `VodCategoryPickerDialog` (acionado a partir das pílulas clássicas) ainda os utilizam.
- [ ] Os composables `VodHeroStrip` e `VodActionChipRow` em `app/src/main/java/app/ehtudo/iptv/ui/components/shell/VodChrome.kt` permanecem intactos — outros consumidores continuam reaproveitando-os.
- [ ] Strings usadas apenas pelo hero/pílulas removidos (`library_full_browse_title_movies`, `library_full_browse_title_series`, `library_full_browse_subtitle`, `movies_categories_title`, `series_categories_title`, `library_lens_continue`, `library_lens_top_rated`, `library_lens_fresh_movies`, `library_lens_fresh_series`, `favorites_title`, `library_saved_items_count`, `movies_library_lens_subtitle`, `series_library_lens_subtitle`, `player_resume`) podem permanecer em `values/strings.xml` e `values-pt/strings.xml` enquanto outros fluxos (modo CLASSIC, picker de categoria, histórico de continue watching) as referenciarem. Não há strings exclusivas do hero/pílulas que precisem ser apagadas.
- [ ] O build `:app:compileDebugKotlin` passa após a mudança (rodar `./gradlew :app:compileDebugKotlin --no-daemon`). Warnings pré-existentes sobre condições tautológicas em `locked && matchedCategory != null` podem continuar presentes — não foram introduzidos por esta fase.

## Fase 4g — Detalhes VOD enxutos (somente Play / Chromecast / Favorito / Trailer quando houver)

Arquivos principais:
- `app/src/main/java/app/ehtudo/iptv/ui/screens/movies/MovieDetailScreen.kt`
- `app/src/main/java/app/ehtudo/iptv/ui/screens/series/SeriesDetailScreen.kt`

- [ ] A linha de ações do **Filme** mostra, nesta ordem: `Play`/`Resume from …` → `Chromecast` → `Trailer` (somente se `hasTrailer`) → Favorito (heart).
- [ ] A linha de ações da **Série** (em `SeriesDetailActions`, exibida quando há `resumeEpisode`) mostra, nesta ordem: `Resume · …`/`Play · …` → `Chromecast` → Favorito.
- [ ] Cada **episódio** (`EpisodeItem`) renderiza apenas o card `EpisodeRowCard` clicável e o botão `Chromecast`. **Não há** botões `Download` ou `Copy URL` no card de episódio.
- [ ] `Copy URL` (`R.string.stream_url_copy`) e `Download` (`R.string.download_button_label`) foram removidos da UI das telas de detalhe — eles não aparecem em nenhuma das três ações (filme / série / episódio).
- [ ] Os parâmetros `onCopyUrl`, `onDownload`, `onCopyEpisodeUrl`, `onDownloadEpisode` foram removidos de `MovieDetailContent`, `MovieDetailHeroText`, `SeriesDetailContent`, `SeriesDetailActions` e `EpisodeItem`.
- [ ] O helper `copyStreamUrlToClipboard(...)` foi removido de `MovieDetailScreen.kt` e `SeriesDetailScreen.kt` (não há mais chamadores); os imports `android.content.ClipData`, `android.content.ClipboardManager`, `kotlinx.coroutines.launch`, `androidx.compose.runtime.rememberCoroutineScope`, `app.ehtudo.domain.model.Result` foram removidos das duas telas.
- [ ] As lambdas `coroutineScope.launch { copyStreamUrlToClipboard(...) }` e o wrapper `copyEpisodeUrl` foram removidos. `val coroutineScope = rememberCoroutineScope()` e `val copyEpisodeUrl: (Episode) -> Unit = ...` também.
- [ ] O foco inicial do `MovieDetailScreen` permanece no botão `Play` (`playButtonFocusRequester.requestFocusSafely(...)`).
- [ ] `viewModel.resolveCopyStreamUrl(...)` / `viewModel.downloadMovie(...)` / `viewModel.downloadEpisode(...)` podem permanecer no ViewModel mesmo sem chamadores na UI — não há remoção no escopo desta fase.
- [ ] `MovieDetailViewModelCastingTest` e `SeriesDetailViewModelCastingTest` continuam passando (`./gradlew :app:testDebugUnitTest --no-daemon`). Esta fase não altera a superfície do ViewModel, então não há testes novos.
- [ ] Strings (`stream_url_copy`, `download_button_label`, `stream_url_clip_label`, `stream_url_copied`, `stream_url_copy_failed`) podem permanecer em `values/strings.xml` e `values-pt/strings.xml` — outros fluxos (ex.: `AddToGroupDialog`, `ContinueWatching`) podem referenciá-las futuramente.

## Fase 4h — Player VOD enxuto (somente ações essenciais no overlay de Filmes/Séries)

Arquivos principais:
- `app/src/main/java/app/ehtudo/iptv/ui/screens/player/overlay/PlayerControlsChrome.kt` (apenas o composable privado `PlayerVodInfo` e o call-site que o invoca a partir de `PlayerBottomBar`).
- `app/src/main/res/values-pt/strings.xml`.

- [ ] A linha de ações rápidas do **player de Filmes/Séries** (`PlayerVodInfo`) mostra, nesta ordem: `Mute/Unmute` → `Legendas` (se `subtitleTrackCount > 0`) → `Episódios` (se `showEpisodesAction`, apenas séries) → `External Player` (se `showExternalPlayerAction`) → `Velocidade` (sempre) → `A/V Sync` (se `audioVideoSyncEnabled && !isCastConnected`) → `Transmissão`/`Parar Transmissão` (sempre) → `Proporção (Fit/Stretch/Zoom)` (sempre).
- [ ] Os botões **Qualidade de vídeo**, **Áudio**, **Parar reprodução após**, **Permitir standby após inatividade** e **Imagem em imagem** **não** aparecem no player VOD. O servidor Xtream Eh! IPTV não fornece múltiplas trilhas de áudio/vídeo nem usa esses timers; PiP também foi removido para evitar gravação fora do app.
- [ ] O parâmetro `sleepTimerUiState` foi removido de `PlayerVodInfo` (não há mais timer visível). `PlayerBottomBar` e `PlayerControlsOverlay` continuam recebendo `sleepTimerUiState` para uso do `PlayerLiveInfo` (Live TV não foi alterado nesta fase).
- [ ] Os parâmetros removidos de `PlayerVodInfo`: `audioTrackCount`, `videoQualityCount`, `sleepTimerUiState`, `onOpenAudioTracks`, `onOpenVideoTracks`, `onOpenStopPlaybackTimer`, `onOpenIdleStandbyTimer`, `onEnterPictureInPicture`. O call-site em `PlayerBottomBar` foi ajustado removendo apenas esses argumentos — `PlayerBottomBar` ainda os aceita para repassar ao `PlayerLiveInfo`.
- [ ] Tradução PT corrigida: `player_cast` e `player_action_cast` em `values-pt/strings.xml` mudaram de `Elenco` (que significa "elenco de atores") para `Transmissão` (termo correto para o recurso de cast/Chromecast). A variante EN permanece `Cast`/`Chromecast`. A variante `values-es/strings.xml` ainda tem `Elenco` mas não foi corrigida nesta fase (PT é o locale principal do revendedor).
- [ ] **Ressalva importante:** o `MainActivity.onUserLeaveHint()` (`app/src/main/java/app/ehtudo/iptv/MainActivity.kt:210-213`) ainda tenta entrar em PiP automaticamente quando o usuário sai do app durante a reprodução. Esta fase remove apenas o **botão** de PiP, não o comportamento automático. Removê-lo de fato exige mudar o manifesto, `MainActivity` e `enterPlayerPictureInPictureModeIfEligible` — fora do escopo desta fase.
- [ ] **Ressalva Live TV:** os mesmos botões (`Áudio`, `Qualidade`, `Stop Playback`, `Standby`, `PiP`) continuam aparecendo no overlay de Live TV (`PlayerLiveInfo` em `PlayerControlsChrome.kt:786-836` e no `PlayerChannelInfoOverlay`). Esta fase toca **somente** Filmes/Séries. Para aplicar a Live TV, abrir uma nova fase.
- [ ] `viewModel.selectAudioTrack(...)`, `viewModel.selectVideoQuality(...)`, `viewModel.setStopPlaybackTimer(...)`, `viewModel.setIdleStandbyTimer(...)`, `viewModel.enterPictureInPicture(...)` continuam existindo nos ViewModels/Actions. Esta fase remove apenas a UI — as APIs permanecem disponíveis.
- [ ] Build `:app:compileDebugKotlin` e `:app:testDebugUnitTest` passam.

## Fase 4i — Player IPTV enxuto (Channel Info + Live controls sem Subs/Audio/Rec/C-UP/Split/PiP)

Arquivos principais:
- `app/src/main/java/app/ehtudo/iptv/ui/screens/player/overlay/PlayerChannelInfoOverlay.kt` — função `ChannelInfoOverlay`.
- `app/src/main/java/app/ehtudo/iptv/ui/screens/player/overlay/PlayerControlsChrome.kt` — função privada `PlayerLiveInfo` (Live branch do `PlayerBottomBar`).
- `app/src/main/java/app/ehtudo/iptv/ui/screens/player/PlayerScreen.kt` — call-site de `ChannelInfoOverlay`.
- `app/src/main/res/values-pt/strings.xml`.

`ChannelInfoOverlay` (overlay que abre ao tocar/pressionar INFO em Live TV):

- [ ] Removidos do `LazyRow` de quick actions: `Subs` (R.string.player_subs), `Audio` (R.string.player_audio), `Split Screen/Multiview` (R.string.player_multiview_short / R.string.player_action_split), `REC` (botão com `icon="REC"`), `C-UP` (botão com `icon="C-UP"` + `R.string.player_catchup_badge`) e `PiP` (R.string.player_pip_short).
- [ ] Removidos os painéis `ChannelInfoPanel.RECORD` e `ChannelInfoPanel.CATCH_UP` (não há mais botão que os acione). O enum `ChannelInfoPanel` agora só tem `LIVE_DVR`.
- [ ] Removidos `recordButtonFocusRequester`, `recordPanelFocusRequester`, `catchUpButtonFocusRequester`, `catchUpPanelFocusRequester` e suas referências no composable.
- [ ] Removidos os parâmetros da assinatura: `currentRecordingStatus`, `onStartRecording`, `onStopRecording`, `onScheduleRecording`, `onScheduleDailyRecording`, `onScheduleWeeklyRecording`, `onRestartProgram`, `onOpenArchive`, `onOpenSplitScreen`, `subtitleTrackCount`, `liveTranslationAvailable`, `audioTrackCount`, `onOpenSubtitleTracks`, `onOpenAudioTracks`, `onEnterPictureInPicture`. Os pills de status `RecordingStatus.RECORDING`/`SCHEDULED` no header também saíram (não há mais gravação no app pelo IPTV player). Pill `R.string.player_catchup_badge` no header também removido (não há mais catch-up).
- [ ] Call-site em `PlayerScreen.kt:1306` ajustado removendo os argumentos correspondentes.
- [ ] Removidos imports `archivePlaybackCapability`, `isArchivePlayable`, `RecordingStatus` (não há mais referências no composable).

`PlayerLiveInfo` (Live branch do `PlayerBottomBar`, controles completos de Live TV):

- [ ] Removido `Picture in picture` da primary actions list.
- [ ] Removidos `Subs`, `Audio`, `Video Quality` e `Multiview/Split Screen` da secondary actions list. A secondary list agora contém apenas `Aspect Ratio` e (condicional) `A/V Sync`.
- [ ] Removidos da assinatura de `PlayerLiveInfo`: `subtitleTrackCount`, `liveTranslationAvailable`, `audioTrackCount`, `videoQualityCount`, `onOpenSubtitleTracks`, `onOpenAudioTracks`, `onOpenVideoTracks`, `onOpenSplitScreen`, `onEnterPictureInPicture`. Call-site em `PlayerBottomBar` ajustado.

Strings (`values-pt/strings.xml`):

- [ ] `player_subs` corrigido de `Subscritores` para `Legendas`. Em PT, `player_subs` significa legendas do vídeo, não "subscribers". As variantes EN permanecem como `Subs`.
- [ ] `player_cast` e `player_action_cast` ajustados de `Transmissão` (definido na Fase 4h) para `Chromecast` (nome de marca usado em todos os locales, igual a `R.string.cast_button_label`). Mantém-se `player_stop_casting` = `Pare de transmitir` (PT) por consistência com a Fase 4h.
- [ ] EN `player_cast` permanece como `Cast` e `player_action_cast` permanece como `Cast` (o usuário pediu apenas para corrigir o termo PT).

Resíduo intencional fora do escopo desta fase:

- `MainActivity.onUserLeaveHint()` (PiP automático quando o usuário sai do app durante a reprodução) continua ativo. Removê-lo exige mudar o manifesto, `MainActivity` e `enterPlayerPictureInPictureModeIfEligible`.
- ViewModels/Actions para gravação (`viewModel.startManualRecording()`, `viewModel.stopCurrentRecording()`, `viewModel.scheduleRecording()`, etc.) continuam existindo, mas não são mais expostos pela UI.

## Fase 4j — ChannelInfoOverlay sem Quality (Live TV player)

Arquivos principais:
- `app/src/main/java/app/ehtudo/iptv/ui/screens/player/overlay/PlayerChannelInfoOverlay.kt` — função `ChannelInfoOverlay`.
- `app/src/main/java/app/ehtudo/iptv/ui/screens/player/PlayerScreen.kt` — call-site de `ChannelInfoOverlay`.

- [ ] Removido do `LazyRow` de quick actions do `ChannelInfoOverlay`: `Quality` (`R.string.player_quality_short` + `R.string.player_action_quality`, acionado por `videoQualityCount > 0`). O servidor Xtream Eh! IPTV não fornece múltiplas trilhas de vídeo para Live TV.
- [ ] Removidos da assinatura do `ChannelInfoOverlay`: `videoQualityCount: Int = 0` e `onOpenVideoTracks: () -> Unit = {}` (eram os únicos consumidores do botão Quality).
- [ ] Call-site em `PlayerScreen.kt:1306` ajustado removendo os argumentos correspondentes (`videoQualityCount = availableVideoQualities.size` e `onOpenVideoTracks = { showTrackSelection = TrackType.VIDEO }`).
- [ ] **Resíduo Live TV:** as funções do ViewModel `viewModel.selectVideoQuality(...)` e o caminho `showTrackSelection = TrackType.VIDEO` continuam existindo; o overlay completo `PlayerControlsOverlay` (acessado por INFO/MENU) ainda passa `videoTracks` para o motor do player — apenas o botão da `ChannelInfoOverlay` foi removido.
- [ ] Build `:app:compileDebugKotlin` passa.

## Fase 5 — Ativação e sincronização

Arquivo principal: `data/src/main/java/app/ehtudo/data/repository/ProviderRepositoryImpl.kt`.

- [ ] Login Xtream salva o provedor como `isActive = true` e `status = ACTIVE`.
- [ ] O caminho de edição também mantém `isActive = true` e `status = ACTIVE`.
- [ ] Após salvar, agenda a retomada da sincronização e o EPG em background.
- [ ] O login retorna após despachar o trabalho, sem esperar o catálogo inteiro.
- [ ] `handleInitialOnboardingSync` continua preservado para M3U, Jellyfin e Stalker.

## Fase 6 — Defaults da experiência Eh!IPTV

Em instalação limpa, confirme os defaults abaixo. O usuário ainda pode alterá-los nas configurações depois.

- [ ] Navegação superior padrão = `[SEARCH, LIVE_TV, MOVIES, SERIES, SETTINGS]`.
- [ ] Tela inicial padrão = `LIVE_TV`.
- [ ] `liveTvChannelMode` padrão = `PRO`.
- [ ] `liveTvQuickFilterVisibility` padrão = `HIDE`.
- [ ] Primeira categoria Live TV = `All Channels` (`ChannelRepository.ALL_CHANNELS_ID`).
- [ ] A área de TV ao vivo não mostra filtros rápidos no primeiro acesso.

Verifique em instalação limpa:

```bash
adb -s d1d1b8f3 shell pm clear app.ehtudo.iptv.debug
```

## Fase 7 — Strings e marca

- [ ] `welcome_brand_title` = `Eh! IPTV`.
- [ ] `welcome_username_hint` = `Usuário`.
- [ ] `welcome_password_hint` = `Senha`.
- [ ] `welcome_save` = `Salvar`.
- [ ] `welcome_username_required` e `welcome_password_required` existem.
- [ ] `settings_providers` = `Eh!IPTV` em `values/strings.xml` e `values-pt/strings.xml`.
- [ ] O texto do botão Salvar é branco no Welcome e em Configurações.
- [ ] O fundo dos dois botões Salvar é azul-claro, preferencialmente `AppColors.BrandStrong`.
- [ ] Ao adicionar novos recursos, atualize os arquivos de tradução necessários.

## Fase 8 — Build e instalação

```bash
./gradlew :app:assembleDebug --no-daemon
adb devices
adb -s d1d1b8f3 install -r app/build/outputs/apk/debug/app-debug.apk
```

- [ ] Build termina com `BUILD SUCCESSFUL`.
- [ ] O APK instala no Xiaomi `d1d1b8f3` sem `INSTALL_FAILED_USER_RESTRICTED`.
- [ ] Use `adb -s` explicitamente se houver mais de um dispositivo conectado.
- [ ] A instalação incremental preserva os dados; use `pm clear` somente quando precisar validar uma instalação limpa.

## Fase 9 — Verificação funcional

1. Instale/limpe o app e abra o Welcome.
2. Confirme o título, os dois campos e o botão Salvar azul-claro com texto branco.
3. Toque em Salvar vazio e confirme `Digite seu usuário`.
4. Informe credenciais válidas e confirme a entrada no app.
5. Abra Configurações e confirme que a categoria se chama `Eh!IPTV`.
6. Exclua o último provedor, se necessário, e confirme que o formulário inline reaparece.
7. Confirme que Combined M3U e Adicionar provedor continuam ocultos.
8. Salve credenciais pelo formulário de Configurações e confirme que o provedor aparece.
8.1. Abra a categoria `Reprodução` e confirme que ela mostra apenas `Live stream format` e o card de teste de velocidade.
8.2. Confirme que o rail lateral só lista `Eh!IPTV`, `Reprodução`, `Privacidade` e `Sobre`. As categorias de Navegação, Gravação, Backup e EPG sources não devem aparecer.
8.3. Abra a categoria `Privacidade` e confirme que ela mostra apenas o toggle `Conteúdo adulto` e o item `Limpar histórico`. Não deve haver nenhuma linha de configuração manual de PIN.
8.4. Com o toggle desligado, confirme que o status lateral é `OCULTO` (nível 3, conteúdo adulto não aparece em nenhuma lista). Ligue o toggle e confirme que o status passa para `BLOQUEADO` (nível 1, conteúdo adulto aparece mas exige PIN). O literal `0000` jamais aparece em tela.
8.5. Com o toggle ligado (BLOQUEADO), abra a lista de Live TV e confirme que as categorias adultas aparecem. Toque em uma delas e confirme que o app exige o PIN configurado antes de abrir o conteúdo.
8.6. Ligue o toggle pela primeira vez após `pm clear` e verifique via `adb shell run-as` que `hasParentalPin` ficou `true`. Tente acessar uma categoria adulta no app e confirme que ela só abre após digitar o PIN.
8.7. Abra o diálogo `Limpar histórico` e confirme que ele dispara `viewModel.clearHistory()`.
8.8. Abra a categoria `Sobre` e confirme que ela mostra apenas `Versão`, `Site` e `Agradecimento`. Toque em `Site` e em `Agradecimento` e confirme que cada um abre o URL correspondente no navegador/handler padrão.
9. Verifique o banco após o login:

```bash
adb -s d1d1b8f3 exec-out run-as app.ehtudo.iptv.debug cat databases/streamvault.db > /tmp/db.sqlite
sqlite3 /tmp/db.sqlite "SELECT id, name, is_active, status, server_url, username FROM providers;"
```

Esperado: nome `Eh! IPTV`, URL `http://dnstv.top/`, `is_active = 1` e `status = ACTIVE`.

10. Aguarde a sincronização e confirme canais Live TV:

```bash
adb -s d1d1b8f3 logcat -d -t 300 | grep -iE "ProviderSync|XtreamIndex|BackgroundEpg"
sqlite3 /tmp/db.sqlite "SELECT COUNT(*) FROM channels;"
```

## Fase 10 — Atualização do roteiro após novas mudanças

Ao reaplicar o roteiro sobre uma nova base StreamVault:

1. Rode `git log --follow -- docs/skill/iptv-reseller-simplification-checklist.md`.
2. Compare os commits posteriores ao último commit que alterou este arquivo.
3. Inspecione `git show <commit>` e incorpore ao roteiro somente mudanças de produto, comandos de build, caminhos e verificações que ainda sejam válidos.
4. Atualize o campo de referência da versão/commit no início deste documento.
5. Rode `git diff --check` e revise o diff completo.
6. Rode `graphify update .` após modificar código; se o comando não existir no ambiente, registre a limitação.

## Regressões conhecidas

| Sintoma | Verificação inicial |
|---|---|
| Salvar não faz nada | Confirme o `onClick` do `TvButton` e a referência do ViewModel. |
| Erro não aparece | Confirme `quickXtreamError`/`error` e as strings de validação. |
| Provedor não aparece em Configurações | Verifique o observer de `providerRepository.getProviders()`. |
| Provedor volta inativo | Consulte `is_active` e `status` no banco e revise o caminho Xtream. |
| Sincronização não inicia | Verifique logs de `ProviderSync`, `XtreamIndex` e `BackgroundEpg`. |
| Combined M3U reaparece | Procure `CombinedM3uProfilesCard` em `SettingsProviderSection.kt`. |
| Botão Salvar perde contraste | Confirme `ButtonDefaults.colors`, `AppColors.BrandStrong` e `Color.White` nos dois cards. |
| Xiaomi recusa instalação | Habilite `Instalar via USB` nas opções do desenvolvedor do aparelho. |
| Reprodução mostra toggles extras | Verifique `SettingsPlaybackSection.kt`; apenas `Live stream format` e `InternetSpeedTestCard` devem ser emitidos. |
| Rail mostra categorias ocultas | Remova as entradas de `SettingsNavigationRail.kt` e ajuste o `if/else` em `SettingsContentPane.kt` para os índices restantes. |
| Privacidade mostra ParentalControlCard ou toggles de Incógnito/Xtream | Reescreva `SettingsPrivacySection.kt` para emitir apenas `AdultContentToggleRow` (Compose local) e o card `Limpar histórico`; apague as chamadas a `toggleIncognitoMode`, `toggleXtreamTextClassification` e `toggleXtreamBase64TextCompatibility`. |
| PIN padrão não é definido ao ligar o toggle | Garanta que `setAdultContentEnabled(true)` chama `preferencesRepository.setParentalPin("0000")` na primeira vez e ajusta `parentalControlLevel` para `3`. |
| Toggle e status visual não sincronizam | Verifique `SettingsPreferenceSnapshotMapper.kt`: `adultContentEnabled` deve ser `parentalControlLevel == 1 || == 2` (LOCKED/PRIVATE). |
| Status lateral mostra `OCULTO`/`BLOQUEADO` invertido | Confirme `settings_adult_content_status_hidden` (= `OCULTO`) e `settings_adult_content_status_locked` (= `BLOQUEADO`) em `strings.xml`/`values-pt`, e que `AdultContentToggleRow` lê `locked` quando o toggle está ligado. |
| Conteúdo adulto não exige PIN quando BLOQUEADO | Confirme `setAdultContentEnabled(true)` chama `setParentalControlLevel(PARENTAL_LEVEL_LOCKED)` (1). O fluxo de PIN em listas/categorias deve continuar exigindo o PIN já configurado. |
| Aparece item "Configurar PIN" na Privacidade | Não deve haver nenhum item para alterar o PIN do conteúdo adulto. Remova `AdultContentConfigurePinRow` e os callbacks `onShowPinDialogChange`/`onPendingActionChange` da assinatura de `settingsPrivacySection(...)`. |
| Literal `0000` aparece na tela de Privacidade | A `subtitle` do toggle não pode mencionar o PIN. Remova qualquer referência a `0000` da `settings_adult_content_subtitle`. |
| Sobre mostra Atualizações/Crash Reports/Build Info | Reescreva `settingsAboutSection(...)` para emitir apenas `SettingsRow(version)` + `ClickableSettingsRow(site)` + `ClickableSettingsRow(acknowledgment)`. Apague os blocos antigos de Updates e Crash Reports. |
| Hero strip ou linha de pílulas reaparecem no topo de Filmes/Séries | Verifique `MoviesScreen.kt` e `SeriesScreen.kt`: os blocos `item(key = "hero") { ... }` e `item(key = "actions") { ... }` na `LazyColumn` do modo preview devem estar ausentes. O `focusRequester(initialFocusRequester)` deve estar no primeiro card de `favorites_row`/`fresh_row`/`top_rated_row`/`catEntries`, não em um `VodHeroStrip`. |
| Foco não cai em nenhum card ao entrar em Filmes/Séries | Confirme que `Modifier.focusRequester(initialFocusRequester)` está sendo anexado ao primeiro card de `favorites_row` via `.then(if (movie.id == fallbackMovieId) Modifier.focusRequester(initialFocusRequester) else Modifier)` e que `fallbackMovieId` não está `null`. Garanta que `FocusRestoreHost` ainda chama `initialContentFocusRequester.requestFocusSafely(...)`. |
| Botões `Copy URL` ou `Download` reaparecem nos detalhes VOD | Verifique `MovieDetailScreen.kt` (ação do filme), `SeriesDetailActions` (ação da série) e `EpisodeItem` (episódios). Em nenhum dos três deve haver `TvButton` lendo `R.string.stream_url_copy` ou `R.string.download_button_label`. A linha da série deve manter `Play`/`Resume · …`, `Chromecast` e o coração; cada episódio mantém apenas `Chromecast`. |
| Detalhes do filme não focam no botão Play | Confirme que `MovieDetailHeroText` continua invocando `TvButton(onClick = onPlay, modifier = Modifier.focusRequester(playButtonFocusRequester), …)` e que `LaunchedEffect(movie.id) { playButtonFocusRequester.requestFocusSafely(...) }` permanece em `MovieDetailContent`. |
| Botões `Qualidade`, `Áudio`, `Parar reprodução`, `Standby` ou `PiP` reaparecem no player VOD | Verifique `PlayerControlsChrome.kt:PlayerVodInfo` (a action list começa em `PlayerActionsChrom.kt:1063` após a Fase 4h). Nenhum `add(PlayerActionSpec(...))` deve referenciar `R.string.player_video_quality`, `R.string.player_audio`, `R.string.player_stop_playback_after`, `R.string.player_idle_standby_after` ou `R.string.player_picture_in_picture`. Live TV ainda tem esses botões — é intencional fora do escopo da Fase 4h. |
| Botão Cast mostra `Elenco` em português | Confirme `values-pt/strings.xml:805` (`player_action_cast` = `Chromecast`) e `:810` (`player_cast` = `Chromecast`). Se algum outro locale (`values-es`, etc.) ainda diz `Elenco`, foi deixado como está nesta fase. |
| Botão `Subs` mostra `Subscritores` em português | Confirme `values-pt/strings.xml:802` (`player_subs` = `Legendas`). `Elenco` e `Subscritores` foram traduzidos errados na Fase 4h e na Fase 4i respectivamente. |
| Botões `Subs`, `Audio`, `Rec`, `C-UP`, `Split` ou `PiP` reaparecem no Channel Info Overlay | Verifique `PlayerChannelInfoOverlay.kt` (LazyRow em torno da linha 380). Nenhum `QuickActionButton` deve referenciar `R.string.player_subs`, `R.string.player_audio`, `R.string.player_multiview_short`, `R.string.player_catchup_badge`, `R.string.player_pip_short` nem usar `icon="REC"` ou `icon="C-UP"`. O enum `ChannelInfoPanel` deve ter apenas `LIVE_DVR`. |
| Botões `Subs`, `Audio`, `Split` ou `PiP` reaparecem no overlay Live completo | Verifique `PlayerLiveInfo` em `PlayerControlsChrome.kt`: primary actions não devem ter `Picture in picture`; secondary actions não devem ter `Subs`, `Audio`, `Video Quality` nem `Multiview`. |
| Botão `Quality` reaparece no Channel Info Overlay | Verifique `PlayerChannelInfoOverlay.kt` LazyRow: não deve haver `QuickActionButton` com `R.string.player_quality_short` nem `R.string.player_action_quality`. Os parâmetros `videoQualityCount` e `onOpenVideoTracks` também devem estar ausentes da assinatura de `ChannelInfoOverlay`. |

## Não fazer

- Não adicionar URL editável ao fluxo simplificado.
- Não mascarar a senha se a especificação exigir texto simples.
- Não adicionar o botão Adicionar provedor novamente à seção simplificada sem revisar este roteiro.
- Não reintroduzir Combined M3U na tela de Configurações sem decisão explícita do produto.
- Não reintroduzir categorias do rail (Navegação, Gravação, Backup, EPG sources) sem revisar este roteiro.
- Não reintroduzir toggles extras de Reprodução além de `Live stream format` e teste de velocidade sem revisar este roteiro.
- Não reintroduzir o `ParentalControlCard` nem os toggles de Incógnito / Xtream name-based adult detection / Xtream Base64 na seção Privacidade sem revisar este roteiro.
- Não reintroduzir Atualizações automáticas, Crash Reports, Build info, GitHub ou Doações na seção Sobre sem revisar este roteiro.
- Não reintroduzir o `hero` strip (`VodHeroStrip`) nem a linha de pílulas (`VodActionChipRow` com `browse_all`/`categories`/`favorites`/`resume`/`top_rated`/`fresh`) no topo das páginas de Filmes e Séries (modo preview) sem revisar este roteiro.
- Não reintroduzir `Copy URL` ou `Download` nas linhas de ação de `MovieDetailScreen`, `SeriesDetailActions` (ação da série) ou `EpisodeItem` sem revisar este roteiro. Cada ação da série/detalhe do filme deve manter apenas Play/Resume, Chromecast e Favorito (e Trailer no filme quando houver); cada episódio mantém apenas Chromecast.
- Não reintroduzir `Qualidade do vídeo`, `Áudio`, `Parar reprodução após`, `Permitir standby após inatividade` ou `Imagem em imagem` no player VOD (`PlayerVodInfo` em `PlayerControlsChrome.kt`) sem revisar este roteiro. Live TV não foi tocado nesta fase — para remover também em Live, abrir nova fase.
- Não reintroduzir o termo `Elenco` para o botão de Cast/Chromecast em `values-pt/strings.xml` — o termo correto é `Chromecast` (literal, como `cast_button_label`). Ver `player_cast` e `player_action_cast`.
- Não reintroduzir `Subscritores` em `player_subs` em `values-pt/strings.xml` — o termo correto é `Legendas`.
- Não reintroduzir os botões `Subs`, `Audio`, `Rec`, `C-UP`, `Split Screen` ou `PiP` no `ChannelInfoOverlay`/`PlayerLiveInfo` (Fase 4i) sem revisar este roteiro. O enum `ChannelInfoPanel` deve continuar com apenas `LIVE_DVR`.
- Não remover a autenticação do `ValidateAndAddProvider`.
- Não bloquear o Welcome aguardando o catálogo inteiro.
- Não commitar credenciais de cliente.
- Não misturar renome de pacote com mudanças visuais e de onboarding no mesmo commit quando uma separação for possível.
