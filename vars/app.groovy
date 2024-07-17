def call() {
    def testComponentNames = ["sales"]
    String webImage = 'image-registry.powerapp.cloud/nitro-web/nitro_web:PR-41030-2dd879328d4932e588f171cf2a3aab9b4b15f41f-8@sha256:2e332066b4fb02245707f89eff2a31c4410568a1f260ee87b0213171f894338c'
    int maxParallelTasks = 3
    String yamlFilePath = 'multicontainer.yaml'

    podTemplate(containers: [containerTemplate(image: 'busybox', name: 'bparent', command: 'cat', ttyEnabled: true, volumes: [
            dynamicPVC(mountPath: '/home/config', requestsSize: '5Gi', storageClassName: 'staging-performance', accessModes: ['ReadWriteMany']),
        ])]) {
        podTemplate(
            cloud: getCloud(),
            label: 'busybox-agent',
            containers: [
                containerTemplate(
                    name: 'busybox',
                    image: 'busybox:latest',
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
                        sh "wget -O /home/config/${outputFile} \"${url}\" --no-check-certificate"
                        // stash includes: "${outputFile}", name: "${stashName}"
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
                            downloadAndStash("https://dl.dropboxusercontent.com/scl/fi/7368hynz2bz75cl13zqlm/database.yml?rlkey=1z4jm6qjvy95jrb11nmhxczlo&st=ry5b6cbq", 'database.yml', 'database-yml')                                                                        
                        }
                    )
                }

                stage('Run Tests') {
                    parallelStagesLoop(testComponentNames, webImage, maxParallelTasks)
                }
            }
        }
    }
}
