def call(String webImage) {
     return [
        containerTemplate(
            name: 'web',
            image: webImage, 
            command: 'cat',
            ttyEnabled: true,         
            envVars: [
                envVar(key: 'DATABASE_GIS_HOST', value: '127.0.0.1'),
                envVar(key: 'DATABASE_GIS_USER', value: 'postgres'),
                envVar(key: 'DATABASE_GIS_PASS', value: 'password'),
                envVar(key: 'DATABASE_GIS_NAME', value: 'power_gis'),
                envVar(key: 'DATABASE_PASS', value: 'talkbox'),
                envVar(key: 'DATABASE_HOST', value: '127.0.0.1'),
                envVar(key: 'REDIS_HOST', value: '127.0.0.1'),
                envVar(key: 'CI', value: 'true'),
                envVar(key: 'RAILS_ENV', value: 'test'),
                envVar(key: 'REDIS_LOGICAL_DB_NAME', value: '0'),
                envVar(key: 'RUBY_VERSION', value: '3.0.6'),
                envVar(key: 'BUNDLER_VERSION', value: '2.4.22'),
                envVar(key: 'RUBY_GEM_VERSION', value: '3.3.14'),
                envVar(key: 'BUNDLE_TO', value: '/home/app/bundle'),
                envVar(key: 'GEM_HOME', value: '/home/app/bundle'),   
                envVar(key: 'BUNDLE_APP_CONFIG', value: '/home/app/bundle'),                    
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
            resourceRequestCpu: '1000m',
            resourceRequestEphemeralStorage: '10Gi',
            resourceLimitMemory: '5Gi',
            resourceLimitCpu: '1000m',
            resourceLimitEphemeralStorage: '10Gi'
        ),
        containerTemplate(
            name: 'redis',
            image: 'redis:7.2.5-alpine',
            envVars: [envVar(key: 'ALLOW_EMPTY_PASSWORD', value: 'true')],
            resourceRequestMemory: '200Mi',
            resourceRequestCpu: '50m',
            resourceLimitMemory: '200Mi',
            resourceLimitCpu: '50m'
        ),
        containerTemplate(
            name: 'db',
            image: 'mysql:8.0.32',
            envVars: [envVar(key: 'MYSQL_ROOT_PASSWORD', value: 'talkbox')],
            resourceRequestMemory: '10Gi',
            resourceRequestCpu: '1000m',
            resourceLimitMemory: '10Gi',
            resourceLimitCpu: '1000m'
        ),
    ]
}
