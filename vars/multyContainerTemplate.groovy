def call(index) {
    return containerTemplate(
        name: 'busybox',
        image: 'busybox',
        command: 'cat',
        ttyEnabled: true,
        resourceRequestMemory: '2Gi',
        resourceRequestCpu: '2000m',
        resourceLimitMemory: '2Gi',
        resourceRequestEphemeralStorage: '2Gi',
        resourceLimitEphemeralStorage: '2Gi'
    )
}
