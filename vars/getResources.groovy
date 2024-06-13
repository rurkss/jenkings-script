def call() {
    return [
        limits: [
            memory: '1Gi'
        ],
        requests: [
            cpu: '500m',
            memory: '500Mi'
        ]
    ]
}
