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
                resourceLimitMemory: getResources().limits.memory
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

    int maxParallelTasks = 3
    int totalTasks = env.LOOP_COUNT.toInteger()
    int stagesRequired = (totalTasks + maxParallelTasks - 1) / maxParallelTasks // Ceiling division

    for (int stageIndex = 0; stageIndex < stagesRequired; stageIndex++) {
        stage("Parallel Stage ${stageIndex + 1}") {
            def parallelStages = [:]
            for (int i = 0; i < maxParallelTasks; i++) {
                int taskIndex = stageIndex * maxParallelTasks + i
                if (taskIndex < totalTasks) {
                    def index = taskIndex // need to capture the loop variable
                    parallelStages["Nested Pod Task ${index + 1}"] = {
                        podTemplate(
                            cloud: getCloud(),
                            label: "nested-agent-${index}",
                            containers: [
                                multyContainerTemplate(index)
                            ]
                        ) {
                            node("nested-agent-${index}") {
                                container('busybox') {
                                    stage('Unstash Artifacts in Nested Pod') {
                                        script {
                                            echo "Files before unstashing:"
                                            sh 'ls -l'
                                            unstash 'artifacts-tar-gz'
                                            echo "Files after unstashing:"
                                            sh 'ls -l'
                                        }
                                    }
                                    stage('Extract Artifacts in Nested Pod') {
                                        sh '''
                                            tar -xf artifacts.tar.gz
                                            echo "Current working directory after decompressing the archive:"
                                            pwd
                                            ls -l
                                        '''
                                    }
                                }
                            }
                        }
                    }
                }
            }
            parallel parallelStages
        }
    }
}
