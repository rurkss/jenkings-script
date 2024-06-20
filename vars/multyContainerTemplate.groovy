def call(String webImage) {
    return [
        containerTemplate(
            name: 'web',
            image: 'busybox', 
            command: 'cat',
            ttyEnabled: true,         
            envVars: [
                envVar(key: 'DATABASE_GIS_HOST', value: '127.0.0.1'),
                envVar(key: 'DATABASE_GIS_USER', value: 'postgres'),
                envVar(key: 'DATABASE_GIS_PASS', value: 'password'),
                envVar(key: 'DATABASE_GIS_NAME', value: 'power_gis'),
                envVar(key: 'DATABASE_PASS', value: 'talkbox'),
                envVar(key: 'DATABASE_HOST', value: '127.0.0.1'),
                envVar(key: 'CI', value: 'true'),
                envVar(key: 'RAILS_ENV', value: 'test'),
                envVar(key: 'REDIS_LOGICAL_DB_NAME', value: '0'),
                envVar(key: 'RUBY_VERSION', value: '3.0.6'),
                envVar(key: 'BUNDLER_VERSION', value: '2.4.22'),
                envVar(key: 'RUBY_GEM_VERSION', value: '3.3.14'),
                envVar(key: 'BUNDLE_TO', value: '/usr/local/bundle'),
                envVar(key: 'BUNDLE_FROZEN', value: 'true'),
                envVar(key: 'NODE_OPTIONS', value: '--max-old-space-size=8192 --openssl-legacy-provider'),
                envVar(key: 'NODE_VERSION', value: 'v20.14.0'),
                envVar(key: 'BABEL_DISABLE_CACHE', value: '0'),
                envVar(key: 'CYPRESS_CACHE_FOLDER', value: '/home/app/.cache/yarn/yarn/Cypress'),
                envVar(key: 'NVM_DIR', value: '/home/app/.nvm'),
                envVar(key: 'NODE_BIN', value: '/home/app/.nvm/versions/node/v20.14.0/bin'),
                envVar(key: 'HOME', value: '/home/app')
            ],
            resourceRequestMemory: '5Gi',
            resourceRequestCpu: '2000m',
            resourceRequestEphemeralStorage: '5Gi',
            resourceLimitMemory: '5Gi',
            resourceLimitCpu: '3000m',
            resourceLimitEphemeralStorage: '5Gi'
        ),
        containerTemplate(
            name: 'redis',
            image: 'redis:7.2.5-alpine',
            envVars: [envVar(key: 'ALLOW_EMPTY_PASSWORD', value: 'true')],
            resourceRequestMemory: '256Mi',
            resourceRequestCpu: '100m',
            resourceLimitMemory: '512Mi',
            resourceLimitCpu: '200m'
        ),
        containerTemplate(
            name: 'db',
            image: 'mysql:8.0.32',
            envVars: [envVar(key: 'MYSQL_ROOT_PASSWORD', value: 'talkbox')],
            resourceRequestMemory: '1Gi',
            resourceRequestCpu: '500m',
            resourceLimitMemory: '1Gi',
            resourceLimitCpu: '500m'
        ),
        containerTemplate(
            name: 'geocoder',
            image: 'image-registry.powerapp.cloud/power-gis/power-gis:main',
            envVars: [
                envVar(key: 'SKIP_DATA_LOADING', value: 'true'),
                envVar(key: 'POSTGRES_USER', value: 'postgres'),
                envVar(key: 'POSTGRES_PASSWORD', value: 'password'),
                envVar(key: 'POSTGRES_DB', value: 'power_gis'),
                envVar(key: 'NITRO_USER', value: 'nitro'),
                envVar(key: 'NITRO_PASSWORD', value: 'password'),
                envVar(key: 'DB_DUMP_KEY_ID', value: 'KECA4ZKKBSCGP8OUVJO8'),
                envVar(key: 'DB_DUMP_ACCESS_KEY', value: 'WITYe4fhZNGtnOrC5azcDByaZKtJw+hFKIBJU6pO')
            ],
            resourceRequestMemory: '2Gi',
            resourceRequestCpu: '500m',
            resourceRequestEphemeralStorage: '2Gi',
            resourceLimitMemory: '2Gi',
            resourceLimitCpu: '500m',
            resourceLimitEphemeralStorage: '2Gi'
        )
    ]
}
