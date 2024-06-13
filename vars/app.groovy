def call() {
    // Define the first podTemplate for busybox-agent
    podTemplate(
        label: 'busybox-agent',
        containers: [
            containerTemplate(
                name: 'busybox',
                image: 'busybox',
                command: 'cat',
                ttyEnabled: true
            )
        ]
    ) {
        node('busybox-agent') {
            stage('Initial Stage') {
                container('busybox') {
                    // Print the IP address of the busybox-agent pod
                    sh '''
                        echo "Getting IP address of busybox-agent pod"
                        POD_IP=$(hostname -i)
                        echo "busybox-agent Pod IP: $POD_IP"
                        echo $POD_IP > /tmp/busybox-agent-ip.txt
                    '''
                }
            }
        }
    }

    // Define the second podTemplate for nested-agent using busybox image
    podTemplate(
        label: 'nested-agent',
        containers: [
            containerTemplate(
                name: 'busybox',
                image: 'busybox',
                command: 'cat',
                ttyEnabled: true
            )
        ]
    ) {
        node('nested-agent') {
            stage('Run Hello World in Nested Pod') {
                container('busybox') {
                    // Retrieve the IP address of the busybox-agent pod and print it
                    sh '''
                        echo "Reading IP address of busybox-agent pod from file"
                        BUSYBOX_AGENT_IP=$(cat /tmp/busybox-agent-ip.txt)
                        echo "busybox-agent Pod IP: $BUSYBOX_AGENT_IP"
                    '''
                }
            }
            stage('Create Hello World File in Nested Pod') {
                container('busybox') {
                    sh 'echo "Hello, World!" > /tmp/hello.txt'
                }
            }
        }
    }
}
