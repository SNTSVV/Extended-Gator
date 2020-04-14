#!/bin/bash

versions=(210 213 218 221 225 234 244 254 260 267 296 300)
androids=(28 28 28 28 28 28 28 28 28 28 29)

benchmark_dir=~/phdproject/benchmark

app_package="com.simplemobiletools.gallery.pro"
ls $benchmark_dir

for i_v in ${!versions[@]}
do
    (( i_v=i_v+1 ))
    if [ $i_v -lt ${#versions[@]} ]
    then
        compared_v=${versions[i_v-1]}
        test_v=${versions[i_v]}
        android_v=${androids[i_v-1]}
        
        rm -r apk/*
        cp ${benchmark_dir}/${app_package}/original/${app_package}_${test_v}.apk apk/
        cp ${benchmark_dir}/${app_package}/diff/${app_package}_${compared_v}_${test_v}-diff.json apk/
        ./gator a -p apk/${app_package}_${test_v}.apk -outputFile ${benchmark_dir}/${app_package}/appModel/${app_package}_${compared_v}_${test_v}-AppModel.json
    fi
done