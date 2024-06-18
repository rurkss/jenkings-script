def call() {
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
                resourceRequestEphemeralStorage: '1Gi',
                resourceLimitEphemeralStorage: '2Gi'
            )
        ]
    ) {
        node('busybox-agent') {
            stage('Initial Stage') {
                container('busybox') {
                    script {
                        env.LOOP_COUNT = 1
                        echo "Number of loops: ${env.LOOP_COUNT}"
                    }
                }
            }

            stage('Download Artifacts') {
                container('busybox') {
                    sh '''
                        wget -O artifacts.tar.gz "https://l.station307.com/Avh9v23tYkAmeRWJekypLq/jsdeps.tar.gz"
                        echo "Files after downloading:"
                        ls -l
                    '''
                }
            }

            stage('Stash Artifacts') {
                container('busybox') {
                    sh '''
                        echo "Files before stashing:"
                        ls -l
                    '''
                    stash includes: 'artifacts.tar.gz', name: 'artifacts-tar-gz'
                    script {
                        echo "Files stashed as 'artifacts-tar-gz'"
                    }
                }
            }
        }
    }

    parallelStagesLoop(env.LOOP_COUNT.toInteger(), 3)
}
