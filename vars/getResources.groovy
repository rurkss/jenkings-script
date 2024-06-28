def call() {
    return [
        limits: [
            memory: '1Gi'
        ],
        requests: [
            cpu: '100m',
            memory: '500Mi'
        ]
    ]
}
