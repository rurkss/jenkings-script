def call() {
    def testComponentNames = ["sales"]
    String webImage = 'image-registry.powerapp.cloud/nitro-web/nitro_web:ef524524941664af5a6a79e23ad4412ed36743f1@sha256:0c99acd6ac00584ccc90a08800532faedbbcac55908053549974d6bc6a18dab1'
    int maxParallelTasks = 3
    String yamlFilePath = 'multicontainer.yaml'
    String label = 'busybox-agent'
    String artifactImage = 'image-registry.powerapp.cloud/nitro-web/artifacts:d58921468509716048131a73539062ffacaf7592@sha256:0e5c281524ddcc94dc5a9834608b50fb9829d2246c88ee1966a4fe3c94520aba'
    podTemplate(
        cloud: getCloud(),
        label: label,
        serviceAccount: 'jenkins-build',
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
                resourceLimitEphemeralStorage: '512Mi',                
            ),
            containerTemplate(
                name: 'kubectl',
                command: 'cat',
                image: 'rurkss/kubectl:v1.2',
                ttyEnabled: true,
                resourceRequestMemory: "100Mi",
                resourceRequestCpu: "50m",
                resourceLimitMemory: "100Mi",
                resourceRequestEphemeralStorage: '50Mi',
                resourceLimitEphemeralStorage: '50Mi',                
            ),
            containerTemplate(
                name: 'docker',
                image: 'docker:27.0.3-dind',
                ttyEnabled: true,
                resourceRequestMemory: "100Mi",
                resourceRequestCpu: "50m",
                resourceLimitMemory: "100Mi",
                resourceRequestEphemeralStorage: '1Gi',
                resourceLimitEphemeralStorage: '1Gi',                
            )
        ],
        imagePullSecrets: ['image-registry-prod-robot-powerhome'],
        volumes: [
            dynamicPVC(mountPath: '/home/config', requestsSize: '5Gi', storageClassName: 'file', accessModes: 'ReadWriteMany'),
        ]
    ) {
        node('busybox-agent') {
            def downloadAndStash = { String url, String outputFile, String stashName ->
                container('busybox') {
                    sh "wget -O /home/config/${outputFile} \"${url}\" --no-check-certificate"
                    // stash includes: "${outputFile}", name: "${stashName}"
                }
            }

            // stage('Download and Stash YAMLs') {
            //     parallel (
            //         'Config.yml': {
            //             downloadAndStash("https://dl.dropboxusercontent.com/scl/fi/svdpth34f16dfxuohb4ej/config.yml?rlkey=va8hqpr82wp2c5i5g56tc9vmx&st=pr7mhaoy", 'config.yml', 'config-yml')
            //         },
            //         'Ldap.yml': {
            //             downloadAndStash("https://dl.dropboxusercontent.com/scl/fi/v1pe0vri61j0mj0lctquu/ldap.yml?rlkey=nak1v3kqxoam6h4f3kjkbye7g&st=vnbvlqrz", 'ldap.yml', 'ldap-yml')                        
            //         },
            //         'Database.yml': {
            //             downloadAndStash("https://dl.dropboxusercontent.com/scl/fi/7368hynz2bz75cl13zqlm/database.yml?rlkey=1z4jm6qjvy95jrb11nmhxczlo&st=ry5b6cbq", 'database.yml', 'database-yml')                                                                        
            //         }
            //     )
            // }

            // stage('Get PVC'){
            //     container('kubectl') {
            //         withKubeConfig{
            //             sh "kubectl get pvc -l jenkins/label=${label} -o=jsonpath='{.items[*].metadata.name}'"
            //         }
            //     }
            // }

            stage('Artifacts') {
                container('docker') {
                    sh "ls -all"
                }            
            }

            // stage('Run Tests') {
            //     parallelStagesLoop(testComponentNames, webImage, maxParallelTasks)
            // }
        }
    }
}
