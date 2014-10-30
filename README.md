# CMME early music notation editor/viewer

This is the code for the graphical early music transcription tool developed as part of the Computerized Mensural Music Editing Project, in use at [www.cmme.org](http://www.cmme.org).

NB: This is some legacy-ass Java code, cobbled together over many years. I hope eventually to ditch it and rewrite from the ground up as a web app.

## Building

Use [ANT](http://ant.apache.org/). The default task in build.xml compiles JARs into the `dist/` directory.

Signing JARs (I hate Java):
- put new .pfx in `dist/cert`
- update `dist/cmme.properties` with .pfx name and alias (list aliases with `keytool -list -keystore dist/cert/<PFXFILE> -storetype pkcs12`, change alias with `keytool -changealias -alias '<ORIGINAL_ALIAS>' -destalias '<NEW_ALIAS>' -keystore dist/cert/<PFXFILE> -storetype pkcs12`)
- `ant generate-keystore`
- `ant sign-libs`
- `ant dist-applet`

## Running

The `editor` and `viewer` scripts execute the JAR files built in the `dist/` directory by ANT. When the program is run this way, the default location for CMME music files is `dist/data/music`. The sample music data included with the code can be copied over from the `build/` dir. A more extensive collection of music files is available in the [cmme-music](https://github.com/tdumitrescu/cmme-music) Git repository. To use these scores with the distribution version of the software, place the `cmme-music` repo in the `dist/data/music` directory:

```sh
git clone git@github.com:tdumitrescu/cmme-music.git dist/data/music
```

## Tests

What are those?

## Copying

Copyright 1998 - 2014 Theodor Dumitrescu

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
