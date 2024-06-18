def call(index) {
    return containerTemplate(
        name: 'busybox',
        image: 'busybox',
        command: 'cat',
        ttyEnabled: true,
        resourceRequestMemory: '5Gi',
        resourceRequestCpu: '3000m',
        resourceLimitMemory: '5Gi',
        resourceRequestEphemeralStorage: '5Gi',
        resourceLimitEphemeralStorage: '5Gi'
    )
}
