def call() {
    def testComponentNames = ["sales"]
    String webImage = 'image-registry.powerapp.cloud/nitro-web/nitro_web:PR-41175-ef524524941664af5a6a79e23ad4412ed36743f1-1@sha256:0c99acd6ac00584ccc90a08800532faedbbcac55908053549974d6bc6a18dab1'
    int maxParallelTasks = 3
    String yamlFilePath = 'multicontainer.yaml'

    podTemplate(cloud: getCloud(), name: 'busybox-parent', volumes: [
            dynamicPVC(mountPath: '/home/config', requestsSize: '5Gi', storageClassName: 'file', accessModes: 'ReadWriteMany'),
        ], containers: [containerTemplate(image: 'busybox', name: 'bparent', command: 'cat', ttyEnabled: true)]) {
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
