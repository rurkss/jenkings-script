def call() {
    def testComponentNames = ["componentA", "componentB", "componentC", "componentD"]
    int maxParallelTasks = 2

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

    parallelStagesLoop(testComponentNames, maxParallelTasks)
}
