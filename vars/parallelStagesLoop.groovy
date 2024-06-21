def call(def testComponentNames, String webImage, int maxParallelTasks) {
    int totalTasks = testComponentNames.size()
    def semaphore = new java.util.concurrent.Semaphore(maxParallelTasks)

    stage("Parallel Tasks") {
        def parallelStages = [:]
        for (int taskIndex = 0; taskIndex < totalTasks; taskIndex++) {
            def componentName = testComponentNames[taskIndex]
            parallelStages["Test ${componentName}"] = {
                semaphore.acquire()
                try {
                    podTemplate(
                        cloud: getCloud(),
                        label: "nested-agent-${componentName}",
                        containers: multyContainerTemplate(webImage)                                              
                    ) {
                        node("nested-agent-${componentName}") {
                            container('web') {
                                stage("Unstashing Files") {
                                    parallel(
                                        'Config.yml':{
                                            unstash 'config-yml'
                                            sh 'ls -l'
                                        }
                                        'Database.yml':{
                                            unstash 'database-yml'
                                            sh 'ls -l'
                                        }
                                        'Ldap.yml':{
                                            unstash 'ldap-yml'
                                            sh 'ls -l'
                                        }
                                        'JsDeps':{
                                            // unstash 'jsdeps'
                                            sh '''        
                                                wget -O jsdeps.tar.gz "https://dl.dropboxusercontent.com/scl/fi/7i3c35qq8881ikdm75qzw/jsdeps.tar.gz?rlkey=xfg8jtssr64puoecbi0itdzyh&st=ecnugmcl" --no-check-certificate  
                                                echo "Files after downloading:"
                                                ls -l
                                                pwd
                                            '''
                                            sh 'ls -l'
                                        }
                                    )
                                }  
                                stage("Extracting/Copying Artifacts") {
                                    script {
                                        def scriptContent = libraryResource 'scripts/extract_artifacts.sh'
                                        writeFile file: 'extract_artifacts.sh', text: scriptContent
                                        sh '''
                                            ls -all
                                            sh extract_artifacts.sh                                                
                                            echo "Current working directory after running the script:"
                                            pwd
                                            ls -l /home/app/src
                                        '''
                                    }
                                }
                            }
                        }
                    }
                } finally {
                    semaphore.release()
                }
            }
        }
        parallel parallelStages
    }
}
