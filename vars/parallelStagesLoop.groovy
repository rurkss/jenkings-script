def call(totalTasks, maxParallelTasks) {
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
