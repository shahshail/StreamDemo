#!/bin/bash -e

# Compose a release announcement email.
# Call from the deploy script.

version=$(grep -i versionName app/build.gradle | cut -d \" -f 2)
previousTag=$(git tag --sort=-v:refname | awk 'NR==2')
release_target=$(echo $version | cut -d '-' -f 1)
release_branch="release-$release_target"
if [[ $version =~ 'alpha' ]]; then
  release_branch="development"
fi

subject="[ann] TiO Music Android $version"
recipients="'android-music-dev-release@anuvaautomation.com,qatiohome@gmail.com'"

# print the email body
which thunderbird > /dev/null
out=$?
if [ $out -ne 0  ]; then
  cat<<EOF
  I can automatically compose a release announcement email if you install Thunderbird
  with the "Markdown Here" add-on.
  Aborting.
EOF
  exit 1
fi

body="Greetings!

This is a new build of the TiO Music standalone app.

[Direct download][download] (login required)

[Update via Google Play][play-store] (may take several hours to appear)

[Changes][] (developer login required)

TM

_To unsubscribe, remove yourself from the 'android-music-dev-release@tiohome.com' distribution group in Outlook365._

[play-store]: https://play.google.com/apps/testing/com.tiohome.tiomusic
[download]:  
[changes]: https://gitlab.com/tiohome/android/stream_demo/compare/${previousTag}...${release_target}"

thunderbird -compose "to=${recipients},subject=${subject},body='${body}'"
