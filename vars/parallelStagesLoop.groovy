def call(def testComponentNames, String webImage, int maxParallelTasks) {
    int totalTasks = testComponentNames.size()
    int stagesRequired = (totalTasks + maxParallelTasks - 1)

    for (int stageIndex = 0; stageIndex < stagesRequired; stageIndex++) {
        stage("Parallel Stage ${stageIndex + 1}") {
            def parallelStages = [:]
            for (int i = 0; i < maxParallelTasks; i++) {
                int taskIndex = stageIndex * maxParallelTasks + i
                if (taskIndex < totalTasks) {
                    def componentName = testComponentNames[taskIndex] 
                    parallelStages["Test ${componentName}"] = {
                        podTemplate(
                            cloud: getCloud(),
                            label: "nested-agent-${componentName}",
                            containers: multyContainerTemplate(webImage)                            
                        ) {
                            node("nested-agent-${componentName}") {
                                container('redis') {
                                    stage("Download File for ${componentName}") {
                                        sh '''                                            
                                            echo "Files after downloading:"
                                            ls -l
                                        '''
                                    }
                                    // stage("Unstash Artifacts for ${componentName}") {
                                    //     script {
                                    //         echo "Files before unstashing:"
                                    //         sh 'ls -l'
                                    //         unstash 'sample-txt'
                                    //         // unstash 'artifacts-tar-gz'
                                    //         echo "Files after unstashing:"
                                    //         sh 'ls -l'
                                    //     }
                                    // }
                                    // stage("Extract Artifacts for ${componentName}") {
                                    //     script {
                                    //         def scriptContent = libraryResource 'scripts/extract_artifacts.sh'
                                    //         writeFile file: 'extract_artifacts.sh', text: scriptContent
                                    //         sh '''
                                    //             ls -all
                                    //             sh extract_artifacts.sh                                                
                                    //             echo "Current working directory after running the script:"
                                    //             pwd
                                    //             ls -l
                                    //         '''
                                    //     }
                                    // }
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
