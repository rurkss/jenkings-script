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
                        env.LOOP_COUNT = 5
                        echo "Number of loops: ${env.LOOP_COUNT}"
                    }
                }
            }

            stage('Download Artifacts') {
                container('busybox') {
                    sh '''
                        wget -O artifacts.tar.gz "https://github-cloud.githubusercontent.com/alambic/media/734047178/76/16/7616bb61f619d7152fdc362c1944a095babfc8ceb896c4b4a45dc7a82a82866e?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=AKIA5BA2674WPWWEFGQ5%2F20240614%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20240614T162211Z&X-Amz-Expires=3600&X-Amz-Signature=2bb874420110d8aa45a23f41b42fe03f2238025caa8e84a167f6068cd121e82e&X-Amz-SignedHeaders=host&actor_id=0&key_id=0&repo_id=814329025&token=1"
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
                                            tar -xvf artifacts.tar.gz
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
