def call() {
    def testComponentNames = ["componentA"]
    String webImage = 'image-registry.powerapp.cloud/nitro-web/nitro_web:PR-40151-382f565c0c020b6e97f3f5137b622afd8ccefd87-30'
    int maxParallelTasks = 3
    String yamlFilePath = 'multicontainer.yaml'

    podTemplate(
        cloud: getCloud(),
        label: 'busybox-agent',
        containers: [
            containerTemplate(
                name: 'busybox',
                image: 'busybox',
                command: 'cat',
                ttyEnabled: true,
                resourceRequestMemory: getResources().requests.memory,
                resourceRequestCpu: getResources().requests.cpu,
                resourceLimitMemory: getResources().limits.memory,
                resourceRequestEphemeralStorage: '512Mi',
                resourceLimitEphemeralStorage: '512Mi'
            )
        ]
    ) {
        node('busybox-agent') {
            def downloadAndStash = { String url, String outputFile, String stashName ->
                container('busybox') {
                    sh "wget -O ${outputFile} \"${url}\" --no-check-certificate"
                    stash includes: "${outputFile}", name: "${stashName}"
                }
            }

            stage('Download and Stash YAMLs') {
                parallel (
                    'Config.yml': {
                        downloadAndStash("https://dl.dropboxusercontent.com/scl/fi/svdpth34f16dfxuohb4ej/config.yml?rlkey=va8hqpr82wp2c5i5g56tc9vmx&st=pr7mhaoy", 'config.yml', 'config-yml')
                    },
                    'Ldap.yml': {
                        downloadAndStash("https://dl.dropboxusercontent.com/scl/fi/v1pe0vri61j0mj0lctquu/ldap.yml?rlkey=nak1v3kqxoam6h4f3kjkbye7g&st=vnbvlqrz", 'ldap.yml', 'ldap-yml')                        
                    },
                    'Database.yml': {
                        downloadAndStash("https://dl.dropboxusercontent.com/scl/fi/7368hynz2bz75cl13zqlm/database.yml?rlkey=1z4jm6qjvy95jrb11nmhxczlo&st=9njascll", 'database.yml', 'database-yml')                        
                    }
                )
            }
        }
    }

    parallelStagesLoop(testComponentNames, webImage, maxParallelTasks)
}
