# Casa TV

O Casa TV é uma tela inicial para Android TV e Google TV. Use as setas do controle para navegar pelos apps instalados. A interface tem relógio pixelado, tema preto inspirado no [Flix Home](https://github.com/schadogamer127-sys/Flix-Home) e um widget de clima opcional. A tela inicial não tem anúncios nem recomendações.

O descanso de tela exibe uma imagem ou um vídeo que você copia para a TV. O vídeo toca em loop, sem som e sem depender do YouTube.

A versão original foi testada numa Philips Google TV TA7. O APK exige Android 8.0 (API 26) ou superior. As opções de depuração sem fio, o botão Home e o descanso de tela variam conforme o modelo e o firmware. TVs Samsung com Tizen e LG com webOS usam outros sistemas.

## Instalar na TV

1. Baixe **Casa-TV.apk** em [Releases](https://github.com/rmonteverde70-create/casa-tv/releases/latest) e instale o [Android SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools) no computador.
2. Conecte a TV e o computador à mesma rede. Na TV, abra **Opções do desenvolvedor → Depuração sem fio**. Use as portas exibidas na TV, pois pareamento e conexão podem usar portas diferentes. [Guia oficial do ADB](https://developer.android.com/tools/adb#wireless)
3. Execute os comandos abaixo no diretório do APK. Use o código mostrado pela TV para parear. Se houver vários dispositivos conectados, acrescente **-s IP_DA_TV:PORTA_DE_CONEXAO** logo após **adb**.

~~~shell
adb pair IP_DA_TV:PORTA_DE_PAREAMENTO
adb connect IP_DA_TV:PORTA_DE_CONEXAO
adb devices -l
adb install -r Casa-TV.apk
adb shell am start -W -n local.casa.tv/.HomeActivity
~~~

Teste as setas, o botão **OK** e a abertura dos aplicativos antes de trocar o launcher padrão.

## Usar o botão Home

Se a TV oferecer escolha de aplicativo de início, selecione **Casa TV**. Também é possível solicitar a troca pelo ADB.

~~~shell
adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
adb shell cmd package set-home-activity local.casa.tv/.HomeActivity
adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
~~~

Anote o componente exibido pelo primeiro comando. Se a troca funcionar, o último comando mostrará **local.casa.tv/.HomeActivity**. Abra outro app e pressione **Home** no controle para testar a volta. Para desfazer, execute **adb shell cmd package set-home-activity COMPONENTE_ORIGINAL** com o valor anotado.

### Philips Google TV TA7

Na Philips testada, o launcher do Google TV tem prioridade e o assistente de configuração assume o Home se o launcher original for desativado sozinho. Use os comandos abaixo se o aparelho já estiver configurado e os dois pacotes aparecerem no resultado de **adb shell cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME**.

~~~shell
adb shell pm disable-user --user 0 com.google.android.tungsten.setupwraith
adb shell pm disable-user --user 0 com.google.android.apps.tv.launcherx
adb shell cmd package set-home-activity local.casa.tv/.HomeActivity
adb shell input keyevent KEYCODE_HOME
~~~

Isso desativa o launcher e o assistente de configuração originais para o usuário principal. Para reverter, inclusive antes de desinstalar o Casa TV, use estes comandos.

~~~shell
adb shell pm enable --user 0 com.google.android.apps.tv.launcherx
adb shell pm default-state --user 0 com.google.android.tungsten.setupwraith
adb shell cmd package set-home-activity com.google.android.apps.tv.launcherx/.home.HomeActivity
adb shell input keyevent KEYCODE_HOME
~~~

## Personalizar a assinatura e o clima

Copie [casa.properties.example](casa.properties.example) para **casa.properties**. Edite a assinatura, o nome da cidade e as coordenadas decimais. O Casa TV acrescenta um coração à assinatura, quando preenchida. O widget envia as coordenadas configuradas ao [Open-Meteo](https://open-meteo.com/en/docs), que fornece as condições atuais e a mínima e a máxima do dia. A configuração dispensa permissão de localização.

~~~shell
adb shell mkdir -p /sdcard/Android/data/local.casa.tv/files
adb push casa.properties /sdcard/Android/data/local.casa.tv/files/casa.properties
adb shell am force-stop local.casa.tv
adb shell am start -W -n local.casa.tv/.HomeActivity
~~~

## Configurar o descanso de tela

Use um vídeo que a TV consiga reproduzir. MP4 em H.264 é um ponto de partida. Confira o espaço disponível com **adb shell df -h /sdcard**. Para o vídeo, use o nome de destino **screensaver-video** exatamente como no exemplo. O vídeo tem prioridade sobre a imagem. Se houver apenas uma imagem, ela deve se chamar **background.jpg**.

~~~shell
adb shell mkdir -p /sdcard/Android/data/local.casa.tv/files
adb push meu-video.mp4 /sdcard/Android/data/local.casa.tv/files/screensaver-video
~~~

~~~shell
adb push minha-imagem.jpg /sdcard/Android/data/local.casa.tv/files/background.jpg
~~~

Se a TV listar o **Casa TV** como protetor de tela, selecione-o nas configurações. Na Philips TA7, os comandos abaixo ativaram o descanso de tela após três minutos de inatividade. Anote os valores atuais primeiro. A opção **screen_off_timeout** também controla o tempo para apagar a tela no sistema.

~~~shell
adb shell settings get secure screensaver_enabled
adb shell settings get secure screensaver_components
adb shell settings get secure screensaver_activate_on_sleep
adb shell settings get system screen_off_timeout

adb shell settings put secure screensaver_enabled 1
adb shell settings put secure screensaver_components local.casa.tv/.LandscapeDream
adb shell settings put secure screensaver_activate_on_sleep 1
adb shell settings put system screen_off_timeout 180000
~~~

Volte à tela inicial e aguarde três minutos sem usar o controle. Para restaurar a configuração anterior, reaplique os valores anotados com **settings put**. Se algum valor estava como **null**, use **settings delete** para a mesma chave. Aplicativos de vídeo podem manter a tela ativa durante a reprodução.

## Compilar no Windows

Instale PowerShell 7, um JDK 17 ou superior, Android SDK Platform 35 e Android SDK Build-Tools 35.0.0. Defina **JAVA_HOME** e **ANDROID_HOME** ou informe **-JdkRoot** e **-SdkRoot**.

~~~powershell
pwsh -File ./build.ps1
~~~

O script gera **build/Casa-TV.apk** e uma chave local de assinatura na pasta **build**. Guarde a chave para atualizar instalações feitas com seus próprios APKs. O Android exige a mesma assinatura para instalar uma atualização por cima da versão anterior.

Os créditos e as licenças das marcas incluídas estão em [app/src/main/assets/marks](app/src/main/assets/marks).
