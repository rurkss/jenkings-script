def call() {
    def testComponentNames = ["componentA"]
    String webImage = 'image-registry.powerapp.cloud/nitro-web/nitro_web:576f79eb284848004cc6f10c4cf99e299464ae18@sha256:1683130b876fa5bd363d16b67ec51d7de942eeb44befb848b21457366ebb0eb9'
    int maxParallelTasks = 3

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
            stage('Stash Artifacts') {
                container('busybox') {
                    sh '''
                        touch sample.txt
                        echo "Files before stashing:"
                        ls -l
                    '''
                    stash includes: 'sample.txt', name: 'sample-txt'
                    script {
                        echo "Files stashed as 'artifacts-tar-gz'"
                    }
                }
            }
        }
    }

    parallelStagesLoop(testComponentNames, webImage, maxParallelTasks)
}
