#!/bin/bash

function load() {
    local table="$(basename "$1" '.csv')"
    local columns="($(head -n1 $1))"
    echo "set character_set_database=utf8mb4; LOAD DATA LOCAL INFILE '$1' INTO TABLE $table FIELDS TERMINATED BY ',' IGNORE 1 LINES $columns;" \
        | mysql -h localhost -u root -proot -P 3306 demo
}

for file in /docker-entrypoint-initdb.d/csv/*.csv; do
    load ${file}
done
