#!/bin/bash
#
# This script makes build.hxml combined of base-build.hxml and all haxe classes in `src` directory
#
set -e
cd `dirname $0` > /dev/null

RESULT_BUILD=./build.hxml

cd src
haxe_files=`find . -name \*.hx`
haxe_files=${haxe_files//.hx/}
haxe_files=${haxe_files//"./"/}
haxe_files=${haxe_files//"/"/.}
cd ..

cat ./base-build.hxml > $RESULT_BUILD
echo "$haxe_files" >> $RESULT_BUILD
