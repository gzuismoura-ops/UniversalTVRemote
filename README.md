# Universal TV Remote — Android

Projeto Android em Kotlin + Jetpack Compose para descoberta e controle de TVs e dispositivos de mídia na rede local.

## O que este projeto é (e o que não é)

Este aplicativo é um controle multi-protocolo. Ele **não consegue garantir controle de todos os modelos de TV**: fabricantes, gerações, regiões e versões de firmware expõem APIs diferentes. Algumas TVs exigem confirmação física na tela; outras desativam o controle de rede ou não oferecem comandos para canais, teclado ou aplicativos.

A detecção de um dispositivo ou de uma porta aberta não significa que todos os comandos são compatíveis.

## Protocolos presentes no projeto

- LG webOS: SSAP via WebSocket, registro com `client-key`, comandos de áudio/energia e canal de teclas por pointer socket. O registro agora aguarda a resposta `registered` antes de marcar a conexão como autorizada; o endereço do pointer socket é normalizado para `ws://` ou `wss://`.
- Samsung Tizen: Remote Control WebSocket; autorização e disponibilidade variam conforme modelo/configuração.
- Sony Bravia: IRCC-IP / APIs de rede; pode exigir Pre-Shared Key e configuração no aparelho.
- Roku: ECP; algumas configurações e versões restringem controle por aplicativos móveis.
- Vizio SmartCast, Panasonic Viera, Philips JointSpace: APIs específicas de cada família e firmware.
- DLNA/UPnP: comandos de mídia/volume apenas quando o dispositivo anuncia os serviços correspondentes.
- Chromecast DIAL: descoberta/lançamento de aplicativos compatíveis; não substitui um controle remoto completo.
- Android TV / Google TV: a implementação ADB legada não equivale ao pareamento moderno por protocolo Android TV Remote. O pareamento moderno ainda precisa ser implementado para controle amplo dessas TVs.

## Permissões e autorização

O telefone e a TV devem estar na mesma rede local. Ative o controle por rede nas configurações da TV. Quando a TV exibir um pedido de autorização, confirme-o na própria televisão. Não há forma legítima de contornar o pareamento exigido pelo fabricante.

## Compilar APK no GitHub Actions

O workflow `.github/workflows/build-apk.yml` compila `app:assembleDebug` quando há push na branch `main`/`master` ou execução manual. O APK fica no artefato `UniversalTVRemote-debug-apk`.

## Testes necessários antes de considerar um modelo compatível

Para cada marca/modelo/firmware, validar: descoberta, autorização, conexão persistente, setas, OK, voltar, home, volume, mute, canais, teclado numérico, mídia, aplicativos e reconexão após reiniciar a TV. Recursos não oferecidos pela API devem aparecer como indisponíveis, não como controles que aparentam funcionar.
