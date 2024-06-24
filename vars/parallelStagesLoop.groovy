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
                        containers: multyContainerTemplate(webImage),                                          
                    ) {
                        node("nested-agent-${componentName}") {
                            stage('Give Permissions') {                                
                                sh "chmod 777 -R ${env.WORKSPACE}"                                                                    
                            }
                            container('web') {
                                stage("Setup Permissions") {
                                    sh '''
                                        echo "Current User and Groups:"
                                        id
                                    '''
                                }
                                stage("Unstashing Files") {
                                    parallel(
                                        'Config.yml': {
                                            unstash 'config-yml'
                                            sh '''        
                                                mv config.yml /home/app/src/config/
                                                ls -l /home/app/src/config/
                                            '''                                            
                                        },
                                        'Database.yml': {
                                            unstash 'database-yml'
                                            sh '''        
                                                mv database.yml /home/app/src/config/
                                                ls -l /home/app/src/config/
                                            '''
                                        },
                                        'Ldap.yml': {
                                            unstash 'ldap-yml'
                                            sh '''        
                                                mv ldap.yml /home/app/src/config/
                                                ls -l /home/app/src/config/
                                            '''
                                        },
                                        'JsDeps': {
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
                                            ls -l /home/app/src/components/sales
                                            sh extract_artifacts.sh                                                
                                            echo "Current working directory after running the script:"
                                            pwd
                                            ls -l /home/app/src/components/sales
                                        '''
                                    }
                                }
                                stage("Run Test ${componentName}") {
                                    script{
                                        def scriptContent = libraryResource 'scripts/run_test.sh'
                                        writeFile file: 'run_test.sh', text: scriptContent
                                        sh '''
                                            mv run_test.sh /home/app/src
                                            chmod +x /home/app/src/run_test.sh
                                            cd /home/app/src
                                            bash run_test.sh
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
