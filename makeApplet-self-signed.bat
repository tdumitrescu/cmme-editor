jar cvmf manifest.applet cmmeViewer.jar DataStruct/*.class Gfx/*.class Util/*.class Viewer/*.class data/imgs/GUIicons/*.*
keytool -genkey -validity 3650 -keystore pKeyStore -alias CMMEKey
keytool -selfcert -keystore pKeyStore -alias CMMEKey -validity 3650
jarsigner -keystore pKeyStore cmmeViewer.jar CMMEKey