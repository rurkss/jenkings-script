#!/bin/bash

if [ -f "jsdeps.tar.gz" ]; then
    echo "Starting decompression with tar -xf..."
    time tar -xf jsdeps.tar.gz

    if [ -d "node_modules" ]; then
        echo "Moving node_modules to /home/app/src..."
        mv node_modules /home/app/src/
        echo "Move complete."
    else
        echo "node_modules folder not found."
    fi

    if [ -d "components" ]; then
        for dir in components/*; do
            if [ -d "$dir" ]; then
                folder_name=$(basename "$dir")
                if [ -d "$dir/node_modules" ]; then
                    echo "Moving node_modules to /home/app/src/components/$folder_name..."
                    mv "$dir/node_modules" "/home/app/src/components/$folder_name/"
                    echo "Move complete for $folder_name."
                else
                    echo "node_modules not found in $folder_name."
                fi
            fi
        done
    else
        echo "components directory not found."
    fi
    rm -rf jsdeps.tar.gz    
else
    echo "jsdeps.tar.gz not found."
fi
