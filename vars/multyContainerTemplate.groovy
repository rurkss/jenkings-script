def call(index) {
    return containerTemplate(
        name: 'busybox',
        image: 'busybox',
        command: 'cat',
        ttyEnabled: true,
        resourceRequestMemory: getResources().requests.memory,
        resourceRequestCpu: getResources().requests.cpu,
        resourceLimitMemory: getResources().limits.memory
    )
}
