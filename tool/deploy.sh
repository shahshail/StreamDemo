#!/bin/sh -e
app="tiomusic"
version=$(grep -i versionName app/build.gradle | cut -d \" -f 2)
indir="app/build/outputs/apk/release"
outdir="$HOME/Desktop"
targetdir="$outdir/v$version"
outfile="$targetdir/$app-$version.apk"
repo_root="$(git rev-parse --show-toplevel)"

cat<<EOF
  Deploying $version to $targetdir
EOF

mkdir -p "$targetdir"
#cp "$indir/app-debug.apk" "$targetdir/$app-debug-$version.apk"
cp "$indir/app-release.apk" "$outfile"

# compose the release announcement email
cd - > /dev/null
./tool/announce-email.sh
