#!/bin/bash

mkdir build
cd build
cmake ..
[ $? -eq 0 ] || exit $?
make

if [ $? -eq 0 ]
then
  echo " "
  echo "build successful. plugin are generated under following folder:"
  pwd
  ls -l
fi

