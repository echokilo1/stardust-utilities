#!/bin/sh
if [ -d "$1" ] 
then
  l=`find $1 -type f -printf "%p\n"`
  x=1
  y=0
  z=0
  for f in ${l}
  do
    #printf "${f}\n"
    fname=`basename ${f} .txt`
    cat ${f} | java stardust.Reconstruct ${x} > dots/${fname}.dot
    if [ $? -eq 1 ]
    then
      y=$(($y+1))
    else
      x=$(($x+1))
    fi
    z=$(($z+1))
  done
  printf "Total number: $z\n"
  printf "Malformed: $y\n"
fi
